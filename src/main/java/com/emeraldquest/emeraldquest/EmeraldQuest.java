package com.emeraldquest.emeraldquest;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.net.*;

import com.emeraldquest.emeraldquest.commands.*;
import com.mixpanel.mixpanelapi.ClientDelivery;
import com.mixpanel.mixpanelapi.MessageBuilder;
import com.mixpanel.mixpanelapi.MixpanelAPI;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.block.Block;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.Material.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.*;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import org.bukkit.WorldBorder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by explodi on 11/1/15.
 */

public class  EmeraldQuest extends JavaPlugin {
    // TODO: remove env variables not being used anymore
    // Connecting to REDIS
    // Links to the administration account via Environment Variables
    public final static String EMERALDQUEST_ENV = System.getenv("EMERALDQUEST_ENV") != null ? System.getenv("EMERALDQUEST_ENV") : "development";
    public final static UUID ADMIN_UUID = System.getenv("ADMIN_UUID") != null ? UUID.fromString(System.getenv("ADMIN_UUID")) : null;
    
    public final static Integer DENOMINATION_FACTOR = System.getenv("DENOMINATION_FACTOR") != null ? Integer.parseInt(System.getenv("DENOMINATION_FACTOR")) : 1;
//these make it so land price/max and min loot can be set in env
    public final static Integer LAND_PRICE = System.getenv("LAND_PRICE") != null ? Integer.parseInt(System.getenv("LAND_PRICE")) : 15;
	public final static Integer MIN_LOOT = System.getenv("MIN_LOOT") != null ? Integer.parseInt(System.getenv("MIN_LOOT")) : 5;
	public final static Integer MAX_LOOT = System.getenv("MAX_LOOT") != null ? Integer.parseInt(System.getenv("MAX_LOOT")) : LAND_PRICE;
	public final static Integer LOOTIPLIER = System.getenv("LOOTIPLIER") != null ? Integer.parseInt(System.getenv("LOOTIPLIER")) : 5;

    public final static String DENOMINATION_NAME = System.getenv("DENOMINATION_NAME") != null ? System.getenv("DENOMINATION_NAME") : "Emeralds";
    public final static String SET_PvP = System.getenv("SET_PvP") != null ? System.getenv("SET_PvP") : "false";
    
    public final static int MAX_STOCK=100;
    public final static String SERVER_NAME=System.getenv("SERVER_NAME") != null ? System.getenv("SERVER_NAME") : "EmeraldQuest";

   
 // Support for statsd is optional but really cool
    public final static String STATSD_HOST = System.getenv("STATSD_HOST") != null ? System.getenv("STATSD_HOST") : null;
    public final static String STATSD_PREFIX = System.getenv("STATSD_PREFIX") != null ? System.getenv("STATSD_PREFIX") : "emerladquest";
    public final static String STATSD_PORT = System.getenv("STATSD_PORT") != null ? System.getenv("STATSD_PORT") : "8125";
    // Support for mixpanel analytics
    public final static String MIXPANEL_TOKEN = System.getenv("MIXPANEL_TOKEN") != null ? System.getenv("MIXPANEL_TOKEN") : null;
    public MessageBuilder messageBuilder;
    // Support for minecraft-mp vote
    public final static String VOTE_API_KEY = System.getenv("VOTE_API_KEY") != null ? System.getenv("VOTE_API_KEY") : null;
    // REDIS: Look for Environment variables on hostname and port, otherwise defaults to localhost:6379
    public final static String REDIS_HOST = System.getenv("REDIS_1_PORT_6379_TCP_ADDR") != null ? System.getenv("REDIS_1_PORT_6379_TCP_ADDR") : "localhost";
    public final static Integer REDIS_PORT = System.getenv("REDIS_1_PORT_6379_TCP_PORT") != null ? Integer.parseInt(System.getenv("REDIS_1_PORT_6379_TCP_PORT")) : 6379;
    public final static Jedis REDIS = new Jedis(REDIS_HOST, REDIS_PORT);

  public static final String DISCORD_HOOK_URL = System.getenv("DISCORD_HOOK_URL");
  public static final String DISCORD_URL = System.getenv("DISCORD_URL");
  public static final String DISCORD_HOOK_CHANNEL_ID = System.getenv("DISCORD_HOOK_CHANNEL_ID");

    // FAILS
    // public final static JedisPool REDIS_POOL = new JedisPool(new JedisPoolConfig(), REDIS_HOST, REDIS_PORT);

    // Minimum transaction by default is 2000 bits
    
    // utilities: distance and rand
    public static int distance(Location location1, Location location2) {
        return (int) location1.distance(location2);
    }

    public static int rand(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }
    public StatsDClient statsd;
    public Player last_loot_player;
    //public boolean spookyMode=false;
    public boolean rate_limit=false;
    // caches is used to reduce the amounts of calls to redis, storing some chunk information in memory
    public HashMap<String,Boolean> land_unclaimed_cache = new HashMap();
    public HashMap<String,String> land_owner_cache = new HashMap();
    public HashMap<String,String> land_permission_cache = new HashMap();
    public HashMap<String,String> land_name_cache = new HashMap();
    public ArrayList<ItemStack> books=new ArrayList<ItemStack>();
    // when true, server is closed for maintenance and not allowing players to join in.
    public boolean maintenance_mode=false;
    private Map<String, CommandAction> commands;
    private Map<String, CommandAction> modCommands;
    private Map<String, CommandAction> ytCommands;
    private Player[] moderators;
    private Player[] youtubers;
    public Long checkTime60 = new Date().getTime();

    @Override
    public void onEnable() {
        log("EmeraldQuest starting");

        REDIS.set("STARTUP","1");
        REDIS.expire("STARTUP",300);
        if (ADMIN_UUID == null) {
            log("Warning: You haven't designated a super admin. Launch with ADMIN_UUID env variable to set.");
        }
        if(STATSD_HOST!=null && STATSD_PORT!=null) {
            statsd = new NonBlockingStatsDClient("emeraldquest", STATSD_HOST , new Integer(STATSD_PORT));
            System.out.println("StatsD support is on.");
        }
        // registers listener classes
        getServer().getPluginManager().registerEvents(new ChatEvents(this), this);
        getServer().getPluginManager().registerEvents(new BlockEvents(this), this);
        getServer().getPluginManager().registerEvents(new EntityEvents(this), this);
        getServer().getPluginManager().registerEvents(new InventoryEvents(this), this);
        getServer().getPluginManager().registerEvents(new SignEvents(this), this);
        getServer().getPluginManager().registerEvents(new ServerEvents(this), this);

        // player does not lose inventory on death
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule keepInventory on");


        // loads config file. If it doesn't exist, creates it.
        getDataFolder().mkdir();
        if (!new java.io.File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }

       
        // sets the redis save intervals
        REDIS.configSet("SAVE","900 1 300 10 60 10000");
	if(System.getenv("VOTE_API_KEY")!=null) {
		serverInfo();
	}
     
        // Removes all entities on server restart. This is a workaround for when large numbers of entities grash the server. With the release of Minecraft 1.11 and "max entity cramming" this will be unnecesary.
        //     removeAllEntities();
        //killAllVillagers();

        createScheduledTimers();

        // creates scheduled timers (update balances, etc)
        createScheduledTimers();

        commands = new HashMap<String, CommandAction>();

        commands.put("land", new LandCommand(this));
	commands.put("home", new HomeCommand(this));
        commands.put("clan", new ClanCommand());
        commands.put("transfer", new TransferCommand(this));
        commands.put("tip", new TipCommand(this));
        commands.put("vote", new VoteCommand(this));
        commands.put("profession", new ProfessionCommand(this));
        commands.put("spawn", new SpawnCommand(this));
        //commands.put("vote", new VoteCommand(this));
        modCommands = new HashMap<String, CommandAction>();
        modCommands.put("rank", new RankCommand(this));
        modCommands.put("butcher", new ButcherCommand());
        modCommands.put("killAllVillagers", new KillAllVillagersCommand(this));
        modCommands.put("createtp", new CreateTPCommand(this));
        modCommands.put("crashTest", new CrashtestCommand(this));
        modCommands.put("mod", new ModCommand(this));
        modCommands.put("ban", new BanCommand());
        modCommands.put("permban", new PermbanCommand());
        modCommands.put("unban", new UnbanCommand());
        modCommands.put("banlist", new BanlistCommand());
        modCommands.put("spectate", new SpectateCommand(this));
        modCommands.put("tp", new TpCommand(this));
        modCommands.put("emergencystop", new EmergencystopCommand());
        // TODO: Remove this command after migrate.
        modCommands.put("migrateclans", new MigrateClansCommand());
        ytCommands = new HashMap<String, CommandAction>();
        ytCommands.put("spectate", new SpectateCommand(this));
        ytCommands.put("tp", new TpCommand(this));
        ytCommands.put("ban", new BanCommand());
        ytCommands.put("unban", new UnbanCommand());
        ytCommands.put("banlist", new BanlistCommand());
    }
    

  public static void announce(final String message) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.sendMessage(ChatColor.GREEN + message);
    }
  }
  public static void announceIgnore(final String message, final String ignoreWho) {
    for (Player player : Bukkit.getOnlinePlayers()) {
	if(player.getDisplayName() != ignoreWho) {
      player.sendMessage(ChatColor.GREEN + message);	
	}
    }
  }

    public void updateScoreboard(final Player player) throws ParseException, org.json.simple.parser.ParseException, IOException {
            User user=new User(player);
 	
		
                ScoreboardManager scoreboardManager;
                Scoreboard playSBoard;
                Objective playSBoardObj;
                scoreboardManager = Bukkit.getScoreboardManager();
                playSBoard= scoreboardManager.getNewScoreboard();
                playSBoardObj = playSBoard.registerNewObjective("wallet","dummy");

                playSBoardObj.setDisplaySlot(DisplaySlot.SIDEBAR);

                playSBoardObj.setDisplayName(ChatColor.GREEN + ChatColor.BOLD.toString() + "Emerald" + ChatColor.GOLD + ChatColor.BOLD.toString() + "Quest" + ChatColor.GREEN + ".co");

                Score score = playSBoardObj.getScore(ChatColor.GREEN + EmeraldQuest.DENOMINATION_NAME);

        	  int EmAmount=countEmeralds(player);
		
    
		score.setScore(EmAmount);
      		  player.setScoreboard(playSBoard);
            
       
       
    }
    public void teleportToSpawn(Player player) {
        if (!player.hasMetadata("teleporting")) {
            player.sendMessage(ChatColor.GREEN + "Teleporting to spawn...");
            player.setMetadata("teleporting", new FixedMetadataValue(this, true));
            World world = Bukkit.getWorld("world");

            Location location=world.getSpawnLocation();
            location.setX(5);
            location.setY(74);
            location.setZ(0);

            final Location spawn=location;

            Chunk c = spawn.getChunk();
            if (!c.isLoaded()) {
                c.load();
            }
            EmeraldQuest plugin = this;
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(this, new Runnable() {

                public void run() {
                    player.teleport(spawn);
                    player.removeMetadata("teleporting", plugin);
                }
            }, 60L);
        }
    }
    public void teleportToModHQ(Player player) {
        if (!player.hasMetadata("teleporting")) {
            player.sendMessage(ChatColor.GREEN + "Teleporting to MOD-HQ...");
            player.setMetadata("teleporting", new FixedMetadataValue(this, true));
            Location location= new Location(getServer().createWorld(new WorldCreator("MOD-HQ")),0,3,0,0,0);
            System.out.println("location: " + location);
            final Location spawn=location;

            Chunk c = spawn.getChunk();
            System.out.println("Chunk: " + c);
            if (!c.isLoaded()) {
                c.load();
            }
            EmeraldQuest plugin = this;
            BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(this, new Runnable() {

                public void run() {
                    player.teleport(spawn);
                    player.removeMetadata("teleporting", plugin);
                }
            }, 60L);
        }
    }
    public void createScheduledTimers() {
        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getServer().getOnlinePlayers()){
                    User user= null;
                    try {
                        // user.createScoreBoard();
                        updateScoreboard(player);

                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (org.json.simple.parser.ParseException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO: Handle rate limiting
                    }
                }
            }
        }, 0, 120L);
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                // A villager is born
               // World world=Bukkit.getWorld("world");
               // world.spawnEntity(world.getHighestBlockAt(world.getSpawnLocation()).getLocation(), EntityType.VILLAGER);
            }
        }, 0, 72000L);
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                if(statsd!=null) {
                    sendWorldMetrics();
                }
            }
        }, 0, 1200L);

        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                if(statsd!=null) {
                    //updateMetrics();
                }
            }
        }, 0, 12000L);
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                run_season_events();
            }
        }, 0, 1200L);
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                reset_rate_limits();
            }
        }, 0, 100L);

	//discord 1 hour announce
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                long waitTime60 = 1000 * 60 * 60;
		if (checkTime60 <= ((new Date().getTime()) - waitTime60)) {
			sendDiscordMessage("Please Vote here for Rewards! https://minecraft-mp.com/server/189942/vote/");
                }
            }
        }, 0, 1000L);
    }

    public void run_season_events() {
        java.util.Date date= new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int month = cal.get(Calendar.MONTH);
        /*if(month==9) {
            World world=this.getServer().getWorld("world");
            world.setTime(20000);
            world.setStorm(false);
            spookyMode=true;
        } else {
            spookyMode=false;
        }*/
    }
	
	public void sendWorldMetrics() {
        statsd.gauge("players",Bukkit.getServer().getOnlinePlayers().size());
        statsd.gauge("entities_world",Bukkit.getServer().getWorld("world").getEntities().size());
        statsd.gauge("entities_nether",Bukkit.getServer().getWorld("world_nether").getEntities().size());
        statsd.gauge("entities_the_end",Bukkit.getServer().getWorld("world_the_end").getEntities().size());
	
statsd.gauge("entities_MOD-HQ",Bukkit.getServer().getWorld("MOD-HQ").getEntities().size());
    }
    public void recordMetric(String name,int value) {
        if(SERVER_NAME!=null) {
            statsd.gauge("emeraldquest."+SERVER_NAME+"."+name,value);
        }
        System.out.println("["+name+"] "+value);

    }
    

    public void removeAllEntities() {
        World w=Bukkit.getWorld("world");
        List<Entity> entities = w.getEntities();
        int entitiesremoved=0;
        for ( Entity entity : entities){
            entity.remove();
            entitiesremoved=entitiesremoved+1;

        }
        System.out.println("Killed "+entitiesremoved+" entities");
    }
    public void killAllVillagers() {
        World w=Bukkit.getWorld("world");
        List<Entity> entities = w.getEntities();
        int villagerskilled=0;
        for ( Entity entity : entities){
            if ((entity instanceof Villager)) {
                villagerskilled=villagerskilled+1;
                ((Villager)entity).remove();
            }
        }
        w=Bukkit.getWorld("world_nether");
        entities = w.getEntities();
        for ( Entity entity : entities){
            if ((entity instanceof Villager)) {
                villagerskilled=villagerskilled+1;
                ((Villager)entity).remove();
            }
        }
        System.out.println("Killed "+villagerskilled+" villagers");

    }
    public void log(String msg) {
        Bukkit.getLogger().info(msg);
    }

    public void success(Player recipient, String msg) {
        recipient.sendMessage(ChatColor.GREEN + msg);
    }

    public void error(Player recipient, String msg) {
        recipient.sendMessage(ChatColor.RED + msg);
    }
    public int getLevel(int exp) {
        return (int) Math.floor(Math.sqrt(exp / (float)256));
    }
    public int getExpForLevel(int level) {
        return (int) Math.pow(level,2)*256;
    }

    public float getExpProgress(int exp) {
        int level = getLevel(exp);
        int nextlevel = getExpForLevel(level + 1);
        int prevlevel = 0;
        if(level > 0) {
            prevlevel = getExpForLevel(level);
        }
        float progress = ((exp - prevlevel) / (float) (nextlevel - prevlevel));
        return progress;
    }
    public void setTotalExperience(Player player) {
	int rawxp=0;
        if(EmeraldQuest.REDIS.exists("experience.raw."+player.getUniqueId().toString())) {
            rawxp=Integer.parseInt(EmeraldQuest.REDIS.get("experience.raw."+player.getUniqueId().toString()));
        }
        // lower factor, experience is easier to get. you can increase to get the opposite effect
        int level = getLevel(rawxp);
        float progress = getExpProgress(rawxp);

        player.setLevel(level);
        player.setExp(progress);
        setPlayerMaxHealth(player);
    }
    public void setPlayerMaxHealth(Player player) {
        // base health=6
        // level health max=
	int health=4; 
	if (player.getLevel()>=1) {
        	health=4+(player.getLevel()/2);
	}
	if (EmeraldQuest.REDIS.exists("ModFlag "+player.getUniqueId().toString())) {
	if (isModerator(player)&&(EmeraldQuest.REDIS.get("ModFlag "+player.getUniqueId().toString()).equals("true"))){health=20+(player.getLevel()/2); }
	} 
        if(health>40) health=40;
        // player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, player.getLevel(), true));
        player.setMaxHealth(health);
    }

    public void claimLand(final String name, Chunk chunk, final Player player) throws ParseException, org.json.simple.parser.ParseException, IOException {
        // check that land actually has a name
	String chunkname = "";

	if (player.getWorld().getName().equals("world")){
	chunkname="chunk";
	} else if (player.getWorld().getName().equals("world_nether")){
	chunkname="netherchunk";
	} //gets which chunks for which world @bitcoinjake09
        final int x = chunk.getX();
        final int z = chunk.getZ();
        System.out.println("[claim] "+player.getDisplayName()+" wants to claim a plot in "+player.getWorld().getName()+x+","+z+" with name "+name);

        if (!name.isEmpty()) {
            // check that desired area name doesn't have non-alphanumeric characters
            boolean hasNonAlpha = name.matches("^.*[^a-zA-Z0-9 _].*$");
            if (!hasNonAlpha) {
                // 16 characters max
                if (name.length() <= 21) {


                    if (name.equalsIgnoreCase("the wilderness")) {
                        player.sendMessage(ChatColor.RED + "You cannot name your land that.");
                        return;
                    }
                    if (REDIS.get(chunkname+"" + x + "," + z + "owner") == null){
            			User user=new User(player);
                                 player.sendMessage(ChatColor.YELLOW + "Claiming land...");
                        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                        EmeraldQuest emeraldQuest = this;
                        scheduler.runTask(this, new Runnable() {
                            @Override
                            public void run() {
                                // A villager is born
                                try {
                                  //if ((player.getUniqueId().toString().equals(EmeraldQuest.ADMIN_UUID.toString()))||((removeEmeralds(player,(EmeraldQuest.LAND_PRICE / 100))) == true)) {
					String chunkname = "";
					int landiplier = 1;
	if (player.getWorld().getName().equals("world")){
	chunkname="chunk";
	} else if (player.getWorld().getName().equals("world_nether")){
	chunkname="netherchunk";
	landiplier =4;
	} //gets which chunks for which world @bitcoinjake09
	
                                    if (((removeEmeralds(player,(LAND_PRICE*landiplier))) == true)) {

                                        EmeraldQuest.REDIS.set(chunkname+"" + x + "," + z + "owner", player.getUniqueId().toString());
                                        EmeraldQuest.REDIS.set(chunkname+"" + x + "," + z + "name", name);
					land_owner_cache=new HashMap();
                                            land_name_cache=new HashMap();
                                            land_unclaimed_cache=new HashMap();
                                        player.sendMessage(ChatColor.GREEN + "Congratulations! You're now the owner of " + name + "!");
                                        player.sendMessage(ChatColor.YELLOW + "Price was "+(LAND_PRICE*landiplier)+" Emeralds");
                                        if (emeraldQuest.messageBuilder != null) {

                                            // Create an event
                                            org.json.JSONObject sentEvent = emeraldQuest.messageBuilder.event(player.getUniqueId().toString(), "Claim", null);
                                            //org.json.JSONObject sentCharge = emeraldQuest.messageBuilder.trackCharge(player.getUniqueId().toString(), EmeraldQuest.LAND_PRICE / 100, null);


                                            ClientDelivery delivery = new ClientDelivery();
                                            delivery.addMessage(sentEvent);
                                            //delivery.addMessage(sentCharge);


                                            MixpanelAPI mixpanel = new MixpanelAPI();
                                            mixpanel.deliver(delivery);
                                        }
                                    } 
					
					else if (((removeEmeralds(player,(LAND_PRICE*landiplier))) != true)){
                                        //int balance = new User(player).wallet.balance();
                                        if (countEmeralds(player) < (LAND_PRICE*landiplier)) {
                                            player.sendMessage(ChatColor.RED + "You don't have enough money! You need " + ChatColor.BOLD + Math.ceil(((LAND_PRICE*landiplier) - countEmeralds(player))) + ChatColor.RED + " more emeralds.");
                                        } else {
                                            player.sendMessage(ChatColor.RED + "Claim payment failed. Please try again later.");
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                };
                            }
                        });		

                    } else if (REDIS.get(chunkname+""+ x + "," + z + "owner").equals(player.getUniqueId().toString()) || isModerator(player)) {
                        if (name.equals("abandon")) {
                            // Abandon land
                            EmeraldQuest.REDIS.del(chunkname+""+ x + "," + z + "owner");
                            EmeraldQuest.REDIS.del(chunkname+""+ x + "," + z + "name");
			    EmeraldQuest.REDIS.del(chunkname+""+x+","+z+"permissions");
                        } else if (name.startsWith("transfer ") && name.length() > 1) {
                            // If the name starts with "transfer " and has at least one more character,
                            // transfer land
                            final String newOwner = name.substring(9);
                            player.sendMessage(ChatColor.YELLOW + "Transfering land to " + newOwner + "...");

                            if (REDIS.exists("uuid:" + newOwner)) {
                                String newOwnerUUID = REDIS.get("uuid:" + newOwner);
                                EmeraldQuest.REDIS.set(chunkname+"" + x + "," + z + "owner", newOwnerUUID);
                                player.sendMessage(ChatColor.GREEN + "This land now belongs to " + newOwner);
                            } else {
                                player.sendMessage(ChatColor.RED + "Could not find " + newOwner + ". Did you misspell their name?");
                            }

                        } else if (EmeraldQuest.REDIS.get(chunkname+"" + x + "," + z + "name").equals(name)) {
                            player.sendMessage(ChatColor.RED + "You already own this land!");
                        } else {
                            // Rename land
                            player.sendMessage(ChatColor.GREEN + "You renamed this land to " + name + ".");
                            EmeraldQuest.REDIS.set(chunkname+"" + x + "," + z + "name", name);
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED+"Your land name must be 16 characters max");
                }
            } else {
                player.sendMessage(ChatColor.RED+"Your land name must contain only letters and numbers");
            }
        } else {
            player.sendMessage(ChatColor.RED+"Your land must have a name");
        }
    }
    public boolean isOwner(Location location, Player player) {
	String key="";	
	if (player.getWorld().getName().equals("world")){
        key="chunk" + location.getChunk().getX() + "," + location.getChunk().getZ() + "owner";
        if(land_owner_cache.containsKey(key)) {
            if(land_owner_cache.get(key).equals(player.getUniqueId().toString())) {
                return true;
            } else {
                return false;
            }
        } else if (REDIS.get(key).equals(player.getUniqueId().toString())) {
            // player is the owner of the chunk
            return true;
        } 
	
	} else if (player.getWorld().getName().equals("world_nether")){
        key="netherchunk" + location.getChunk().getX() + "," + location.getChunk().getZ() + "owner";
        if(land_owner_cache.containsKey(key)) {
            if(land_owner_cache.get(key).equals(player.getUniqueId().toString())) {
                return true;
            } else {
                return false;
            }
        } else if (REDIS.get(key).equals(player.getUniqueId().toString())) {
            // player is the owner of the chunk
            return true;
        } 
	
	} else {
            return false;
        }
	return false;
    }
public boolean canBuild(Location location, Player player) {
         // returns true if player has permission to build in location
        // TODO: Find out how are we gonna deal with clans and locations, and how/if they are gonna share land resources
	try {	
	if (isModerator(player)){	
		if (EmeraldQuest.REDIS.get("ModFlag "+player.getUniqueId().toString()).equals("true")){return true;}
	}//end mod
	} catch (NullPointerException nullPointer)
		{
                	//System.out.println("modflag: "+nullPointer);
		}		

        if (location.getWorld().getEnvironment().equals(Environment.THE_END)) {
            // If theyre not in the overworld, they cant build
            return false;
        } else if (landIsClaimed(location)) {
            if(isOwner(location,player)) {
                return true;
            } else if(landPermissionCode(location).equals("p")) {
                return true;
            } else if(landPermissionCode(location).equals("pv")) {
                return true;// add land permission pv for public Pvp by @bitcoinjake09
            } else if(landPermissionCode(location).equals("c")==true) {
		if (player.getWorld().getName().equals("world")){
                String owner_uuid=REDIS.get("chunk" + location.getChunk().getX() + "," + location.getChunk().getZ() + "owner");
                System.out.println(owner_uuid);
                String owner_clan=REDIS.get("clan:"+owner_uuid);
                System.out.println(owner_clan);
                String player_clan=REDIS.get("clan:"+player.getUniqueId().toString());
                System.out.println(player_clan);
                if(owner_clan.equals(player_clan)) {
                    return true;
                } else {
                    return false;
                }
		}//end world lol @bitcoinjake09
		else if (player.getWorld().getName().equals("world_nether")){
                String owner_uuid=REDIS.get("netherchunk" + location.getChunk().getX() + "," + location.getChunk().getZ() + "owner");
                System.out.println(owner_uuid);
                String owner_clan=REDIS.get("clan:"+owner_uuid);
                System.out.println(owner_clan);
                String player_clan=REDIS.get("clan:"+player.getUniqueId().toString());
                System.out.println(player_clan);
                if(owner_clan.equals(player_clan)) {
                    return true;
                } else {
                    return false;
                }
		}//world_nether @bitcoinjake09
            } else {
                return false;
            }
        } else {
            return true;
        }
	            return true;
    }
    public String landPermissionCode(Location location) {
        // permission codes:
        // p = public
        // c = clan
	// v = PvP(private cant build) by @bitcoinjake09
	// pv= public PvP(can build) by @bitcoinjake09
        // n = no permissions (private)
	// added netherchunks @bitcoinjake09
	String key = "";
	if (location.getWorld().getName().equals("world")){
        key = "chunk"+location.getChunk().getX()+","+location.getChunk().getZ()+"permissions";
            if(land_permission_cache.containsKey(key)) {
            return land_permission_cache.get(key);
        } else if(REDIS.exists(key)) {
            String code=REDIS.get(key);
            land_permission_cache.put(key,code);
            return code;
        } 
	} else if (location.getWorld().getName().equals("world_nether")){
        key = "netherchunk"+location.getChunk().getX()+","+location.getChunk().getZ()+"permissions";
           if(land_permission_cache.containsKey(key)) {
            return land_permission_cache.get(key);
        } else if(REDIS.exists(key)) {
            String code=REDIS.get(key);
            land_permission_cache.put(key,code);
            return code;
        } 
	}else {
            return "n";
        }
                  return "n";
    }

    public boolean createNewArea(Location location, Player owner, String name, int size) {
        // write the new area to REDIS
        JsonObject areaJSON = new JsonObject();
        areaJSON.addProperty("size", size);
        areaJSON.addProperty("owner", owner.getUniqueId().toString());
        areaJSON.addProperty("name", name);
        areaJSON.addProperty("x", location.getX());
        areaJSON.addProperty("z", location.getZ());
        areaJSON.addProperty("uuid", UUID.randomUUID().toString());
        REDIS.lpush("areas", areaJSON.toString());
        // TODO: Check if redis actually appended the area to list and return the success of the operation
        return true;
    }

     public boolean isModerator(Player player) {
    if (REDIS.sismember("moderators", player.getUniqueId().toString())) {
      return true;
    } else if (ADMIN_UUID != null
        && player.getUniqueId().toString().equals(ADMIN_UUID.toString())) {
      return true;
    } else {
      return false;
    }
}
     public boolean isYoutuber(Player player) {
    if (REDIS.sismember("youtubers", player.getUniqueId().toString())) {
      return true;
    } else {
      return false;
    }
}


    
    public boolean landIsClaimed(Location location) {
	String key="";
	if (location.getWorld().getName().equals("world")){
        key="chunk"+location.getChunk().getX()+","+location.getChunk().getZ()+"owner";
        if(land_unclaimed_cache.containsKey(key)) {
            return false;
        } else if (land_owner_cache.containsKey(key)) {
            return true;
        } else {
            if(REDIS.exists(key)==true) {
                land_owner_cache.put(key,REDIS.get(key));
                return true;
            } else {
                land_unclaimed_cache.put(key,true);
                return false;
            }
        }
	}//end world lmao @bitcoinjake09
	else if (location.getWorld().getName().equals("world_nether")){
	key="netherchunk"+location.getChunk().getX()+","+location.getChunk().getZ()+"owner";
        if(land_unclaimed_cache.containsKey(key)) {
            return false;
        } else if (land_owner_cache.containsKey(key)) {
            return true;
        } else {
            if(REDIS.exists(key)==true) {
                land_owner_cache.put(key,REDIS.get(key));
                return true;
            } else {
                land_unclaimed_cache.put(key,true);
                return false;
            }
        }
	}//end nether
	else {
                land_unclaimed_cache.put(key,true);
                return false;
            }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // we don't allow server commands (yet?)
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            // PLAYER COMMANDS
            for(Map.Entry<String, CommandAction> entry : commands.entrySet()) {
                if (cmd.getName().equalsIgnoreCase(entry.getKey())) {
                    entry.getValue().run(sender, cmd, label, args, player);
                }
            }

            // MODERATOR COMMANDS
            for(Map.Entry<String, CommandAction> entry : modCommands.entrySet()) {
                if (cmd.getName().equalsIgnoreCase(entry.getKey())) {
                    if (isModerator(player)) {
                        entry.getValue().run(sender, cmd, label, args, player);
                    } else {
                        //sender.sendMessage("You don't have enough permissions to execute this command!");
                    }
                }
            }
 		// YT COMMANDS
            for(Map.Entry<String, CommandAction> entry : ytCommands.entrySet()) {
                if (cmd.getName().equalsIgnoreCase(entry.getKey())) {
                    if (isYoutuber(player)) {
                        entry.getValue().run(sender, cmd, label, args, player);
                    } else {
                        //sender.sendMessage("You don't have enough permissions to execute this command!");
                    }
                }
            }
        }
        return true;
    }
   
    public void crashtest() {
        this.setEnabled(false);
    }
    public void reset_rate_limits() {
        rate_limit=false;
    }

	
	// the isPvP function by @bitcoinjake09
public boolean isPvP(Location location) {
		if ((landPermissionCode(location).equals("v")==true)||(landPermissionCode(location).equals("pv")==true))
		if(SET_PvP.equals("true")){return true;}// returns true. it is a pvp or public pvp and if SET_PvP is true

               return false;//not pvp
    }
// end isPvP by @bitcoinjake09
public static int countEmeralds(Player player) {

        ItemStack[] items = player.getInventory().getContents();
        int amount = 0;
        for (int i=0; i<player.getInventory().getSize(); i++) {
	ItemStack TempStack = items[i];	
	if ((TempStack != null) && (TempStack.getType() != Material.AIR)){          
	if (TempStack.getType().toString() == "EMERALD_BLOCK") {
                amount += (TempStack.getAmount()*9);
            }
	else if (TempStack.getType().toString() == "EMERALD") {
                amount += TempStack.getAmount();
            }
		}
        }
        return amount;
    }//end count emerald in player inventory by @bitcoinjake09
public boolean removeEmeralds(Player player,int amount){
	 int EmCount = countEmeralds(player);
	 int LessEmCount = countEmeralds(player)-amount;
	 double TempAmount=(double)amount;
	int EmsBack=0;
	ItemStack[] items = player.getInventory().getContents();
	if (countEmeralds(player)>=amount){	
		while(TempAmount>0){		
		for (int i=0; i<player.getInventory().getSize(); i++) {
			ItemStack TempStack = items[i];	
			
			if ((TempStack != null) && (TempStack.getType() != Material.AIR)){          	
			
			if ((TempStack.getType().toString() == "EMERALD_BLOCK")&&(TempAmount>=9)) {
		    player.getInventory().removeItem(new ItemStack(Material.EMERALD_BLOCK, 1));	
        			TempAmount=TempAmount-9;
				}
			if ((TempStack.getType().toString() == "EMERALD_BLOCK")&&(TempAmount<9)) {
		    player.getInventory().removeItem(new ItemStack(Material.EMERALD_BLOCK, 1));	
				EmsBack=(9-(int)TempAmount);  //if 8, ems back = 1      		
				TempAmount=TempAmount-TempAmount;
				if (EmsBack>0) {player.getInventory().addItem(new ItemStack(Material.EMERALD, EmsBack));}
				}
			if ((TempStack.getType().toString() == "EMERALD")&&(TempAmount>=1)) {
      		          player.getInventory().removeItem(new ItemStack(Material.EMERALD, 1));		
        			TempAmount=TempAmount-1;
				}
			
			}//end if != Material.AIR
			
		
	}// end for loop
	}//end while loop
	}//end (EmCount>=amount)
	EmCount = countEmeralds(player);
	if ((EmCount==LessEmCount)||(TempAmount==0))
	return true;	
	return false;
}//end of remove emeralds
//start addemeralds to inventory
public boolean addEmeralds(Player player,int amount){
	int EmCount = countEmeralds(player);
	 int moreEmCount = countEmeralds(player)+amount;
	 double bits = (double)amount;
	 double TempAmount=(double)amount;
	int EmsBack=0;
		while(TempAmount>=0){		
			    	if (TempAmount>=9){		
				TempAmount=TempAmount-9;
				player.getInventory().addItem(new ItemStack(Material.EMERALD_BLOCK, 1));
				}
				if (TempAmount<9){	
				TempAmount=TempAmount-1;
				player.getInventory().addItem(new ItemStack(Material.EMERALD, 1));
				}
			EmCount = countEmeralds(player);
			if ((EmCount==moreEmCount))
			return true;
			}//end while loop
	return false;
}


public boolean savePlayerWorldDatas(Player player) {
	try {
	String saveWorld = player.getWorld().getName();

	ItemStack[] inventoryContentsItemStack = player.getInventory().getContents();
	getConfig().set("inventoryContents"+player.getUniqueId().toString()+saveWorld, inventoryContentsItemStack);

	ItemStack[] armourContentsItemStack = player.getInventory().getArmorContents();
	getConfig().set("armourContents"+player.getUniqueId().toString()+saveWorld, armourContentsItemStack);

	ItemStack[] enderChestContentsItemStack = player.getEnderChest().getContents();
	getConfig().set("enderChestContents"+player.getUniqueId().toString()+saveWorld, enderChestContentsItemStack);


	REDIS.set("xplevel"+player.getUniqueId().toString()+saveWorld, Integer.toString(player.getLevel()));
        saveConfig();

	REDIS.set("locationX"+player.getUniqueId().toString()+saveWorld, String.valueOf(player.getLocation().getX()));
	REDIS.set("locationZ"+player.getUniqueId().toString()+saveWorld, String.valueOf(player.getLocation().getZ()));
	REDIS.set("locationY"+player.getUniqueId().toString()+saveWorld, String.valueOf(player.getLocation().getY()));

	getConfig().set("BedSpawnLocation"+player.getUniqueId().toString()+saveWorld, player.getBedSpawnLocation());


        saveConfig();
		return true;
	} catch (Exception exs) {
		System.out.println(exs);
	}
	return false;
}

public boolean loadPlayerWorldDatas(Player player, String loadWorld) {
	try {
	if (getConfig().get("inventoryContents"+player.getUniqueId().toString()+loadWorld) != null)
	player.getInventory().setContents((ItemStack[]) getConfig().get("inventoryContents"+player.getUniqueId().toString()+loadWorld));

	if (getConfig().get("armourContents"+player.getUniqueId().toString()+loadWorld) != null)
	player.getInventory().setArmorContents((ItemStack[]) getConfig().get("armourContents"+player.getUniqueId().toString()+loadWorld));

	if (getConfig().get("enderChestContents"+player.getUniqueId().toString()+loadWorld) != null)
	player.getEnderChest().setContents((ItemStack[]) getConfig().get("enderChestContents"+player.getUniqueId().toString()+loadWorld));

       if (REDIS.get("xplevel"+player.getUniqueId().toString()+loadWorld) != null) player.setTotalExperience(Integer.parseInt(REDIS.get("xplevel"+player.getUniqueId().toString()+loadWorld)));

	player.setBedSpawnLocation((Location) 	getConfig().get("BedSpawnLocation"+player.getUniqueId().toString()+loadWorld));

		return true;
	} catch (Exception exs) {
		System.out.println(exs);
	}
	return false;
}

  public void sendDiscordMessage(String content) {
    if(System.getenv("DISCORD_HOOK_URL")!=null) {
      //System.out.println("[discord] "+content);
      try {
          String json = "{\"content\":\""+content+"\"}";

          JSONParser parser = new JSONParser();

          final JSONObject jsonObject = new JSONObject();
          jsonObject.put("content", content);
          CookieHandler.setDefault(new CookieManager());

          URL url = new URL(System.getenv("DISCORD_HOOK_URL"));
          HttpsURLConnection con = null;

          System.setProperty("http.agent", "");

          con = (HttpsURLConnection) url.openConnection();

          con.setRequestMethod("POST");
          con.setRequestProperty("Content-Type", "application/json");
          con.setRequestProperty("Cookie", ""+SERVER_NAME+"=true");
          con.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

          con.setDoOutput(true);
          OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
          out.write(json);
          out.close();
	if(con.getResponseCode()==200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
              response.append(inputLine);
            }
            in.close();
            //System.out.println(response.toString());
            //return true;
          } else {
            //return false;
          }
          

      } catch (Exception e) {
          e.printStackTrace();
          //return false;
      }
    }
    //return false;

  } // EO discord
  public boolean giveVoteReward(Player player) {
	boolean hasOpenSlots = false;
        for (ItemStack item : player.getInventory().getContents()) {
	        if (item == null) {
	                hasOpenSlots = true;
                        break;
                }
        }
	if (hasOpenSlots) {
	ItemStack item = new ItemStack(Material.AIR, 1);
		final int whatLoot = rand(1, 100);
		if (whatLoot<=1){
			item = new ItemStack(Material.VILLAGER_SPAWN_EGG, 1);
		} else if (whatLoot==5){
			item = new ItemStack(Material.ELYTRA, 1);
		} else if (whatLoot==6){
			item = new ItemStack(Material.FIREWORK_ROCKET, 64);
		} else if (whatLoot==7){
		announceIgnore(player.getName()+" Just got " + LAND_PRICE + "e for Voting!",player.getName());
		announceIgnore("Please Vote here for Rewards! https://minecraft-mp.com/server/189942/vote/",player.getName());
		player.sendMessage(ChatColor.AQUA + "You just got " + LAND_PRICE + "e for Voting!");
			addEmeralds(player,LAND_PRICE);
			sendDiscordMessage(player.getName()+" Just got " + LAND_PRICE + "e for Voting!");
			sendDiscordMessage("Please Vote here for Rewards! https://minecraft-mp.com/server/189942/vote/");
			return true;
		} else if (whatLoot==8){
			item = new ItemStack(Material.EXPERIENCE_BOTTLE, 16);
		} else if (whatLoot==9){
			item = new ItemStack(Material.GOLDEN_APPLE, 1, (short)1);
		} else if (whatLoot==10){
			item = new ItemStack(Material.MOOSHROOM_SPAWN_EGG, 1);
		} else if (whatLoot==11){
			item = new ItemStack(Material.PARROT_SPAWN_EGG, 1);
		} else if (whatLoot==12){
			item = new ItemStack(Material.TURTLE_SPAWN_EGG, 1);
		} else if (whatLoot==13){
			item = new ItemStack(Material.ZOMBIE_HORSE_SPAWN_EGG , 1);
		} else if (whatLoot==14){
			item = new ItemStack(Material.MULE_SPAWN_EGG , 1);
		} else if (whatLoot==15){
			item = new ItemStack(Material.LLAMA_SPAWN_EGG, 1);
		} else if (whatLoot==16){
			item = new ItemStack(Material.HORSE_SPAWN_EGG, 1);
		} else if (whatLoot==17){
			item = new ItemStack(Material.DONKEY_SPAWN_EGG, 1);
		} else if (whatLoot==18){
			item = new ItemStack(Material.DOLPHIN_SPAWN_EGG , 1);
		}  else if (whatLoot==19) {
			item = new ItemStack(Material.SKELETON_HORSE_SPAWN_EGG , 1);
		}  else if (whatLoot==20) {
			item = new ItemStack(Material.OCELOT_SPAWN_EGG , 1);
		}  else if (whatLoot==21) {
			item = new ItemStack(Material.GUARDIAN_SPAWN_EGG , 1);
		}  else if (whatLoot==22) {
			item = new ItemStack(Material.BAT_SPAWN_EGG , 1);
		} else {
			announceIgnore(player.getName()+" Just got " + (LAND_PRICE/4) + "e for Voting!",player.getName());
			announceIgnore("Please Vote here for Rewards! https://minecraft-mp.com/server/189942/vote/",player.getName());
			player.sendMessage(ChatColor.AQUA + "You just got " + (LAND_PRICE/4) + "e for Voting!");
			addEmeralds(player,(LAND_PRICE/4));
			sendDiscordMessage(player.getName()+" Just got " + (LAND_PRICE/4) + "e for Voting!");
			sendDiscordMessage("Please Vote here for Rewards! https://minecraft-mp.com/server/189942/vote/");
			return true;
		}
	Integer amount = item.getAmount();
	String tempString = ""+item.getType();
        String rs = tempString.replace("_"," "); // Replace _
  	String itemWon = rs;
	announceIgnore(player.getName()+" Just got " + amount + " " + itemWon + " for Voting!",player.getName());
	announceIgnore("Please Vote here for Rewards! https://minecraft-mp.com/server/189942/vote/",player.getName());
	player.sendMessage(ChatColor.AQUA + "You just got " + amount + " " + itemWon + " for Voting!");
	player.getInventory().addItem(item);
	sendDiscordMessage(player.getName()+" Just got " + amount + " " + itemWon + " for Voting!");
	sendDiscordMessage("Please Vote here for Rewards! https://minecraft-mp.com/server/189942/vote/");
	return true;
	}
  return false;
  }


  public int didVote(String playerName) {
    if(System.getenv("VOTE_API_KEY")!=null) {
      //System.out.println("[discord] "+content);
      try {
          //String json = "{\"content\":\""+content+"\"}";

          //JSONParser parser = new JSONParser();

          //final JSONObject jsonObject = new JSONObject();
          //jsonObject.put("content", content);
          CookieHandler.setDefault(new CookieManager());

          URL url = new URL("https://minecraft-mp.com/api/?object=votes&element=claim&key=" + VOTE_API_KEY + "&username=" + playerName + "");
          HttpsURLConnection con = null;

          System.setProperty("http.agent", "");

          con = (HttpsURLConnection) url.openConnection();

          con.setRequestMethod("POST");
          con.setRequestProperty("Content-Type", "application/json");
          con.setRequestProperty("Cookie", ""+SERVER_NAME+"=true");
          con.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

          con.setDoOutput(true);
          //OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
          //out.write(json);
          //out.close();
	if(con.getResponseCode()==200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
              response.append(inputLine);
            }
            in.close();
            //System.out.println(response.toString());
            return Integer.parseInt(response.toString());
          } else {
            return 0;
          }
          

      } catch (Exception e) {
          e.printStackTrace();
          return 0;
      }
    }
    return 0;

  }
  public int claimVote(String playerName) {
    if(System.getenv("VOTE_API_KEY")!=null) {
      //System.out.println("[discord] "+content);
      try {
          //String json = "{\"content\":\""+content+"\"}";

          //JSONParser parser = new JSONParser();

          //final JSONObject jsonObject = new JSONObject();
          //jsonObject.put("content", content);
          CookieHandler.setDefault(new CookieManager());

          URL url = new URL("https://minecraft-mp.com/api/?action=post&object=votes&element=claim&key=" + VOTE_API_KEY + "&username=" + playerName + "");
          HttpsURLConnection con = null;

          System.setProperty("http.agent", "");

          con = (HttpsURLConnection) url.openConnection();

          con.setRequestMethod("POST");
          con.setRequestProperty("Content-Type", "application/json");
          con.setRequestProperty("Cookie", ""+SERVER_NAME+"=true");
          con.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

          con.setDoOutput(true);
          //OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
          //out.write(json);
          //out.close();
	if(con.getResponseCode()==200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
              response.append(inputLine);
            }
            in.close();
            //System.out.println(response.toString());
            return Integer.parseInt(response.toString());
          } else {
            return 0;
          }
          

      } catch (Exception e) {
          e.printStackTrace();
          return 0;
      }
    }
    return 0;

  }
  public void serverInfo() {
    if(System.getenv("VOTE_API_KEY")!=null) {
      //System.out.println("[discord] "+content);
      try {
          //String json = "{\"content\":\""+content+"\"}";

          //JSONParser parser = new JSONParser();

          //final JSONObject jsonObject = new JSONObject();
          //jsonObject.put("content", content);
          CookieHandler.setDefault(new CookieManager());

          URL url = new URL("https://minecraft-mp.com/api/?object=servers&element=detail&key="+VOTE_API_KEY+"");
          HttpsURLConnection con = null;

          System.setProperty("http.agent", "");

          con = (HttpsURLConnection) url.openConnection();

          con.setRequestMethod("POST");
          con.setRequestProperty("Content-Type", "application/json");
          con.setRequestProperty("Cookie", ""+SERVER_NAME+"=true");
          con.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

          con.setDoOutput(true);
          //OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
          //out.write(json);
          //out.close();
	if(con.getResponseCode()==200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
              response.append(inputLine);
            }
            in.close();
            System.out.println(response.toString());
            //return true;
          } else {
            //return false;
          }
          

      } catch (Exception e) {
          e.printStackTrace();
          //return false;
      }
    }
    //return false;

  }//EO voting

} //EOF
