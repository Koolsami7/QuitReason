package host.sami.quitreason;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;



public class Main extends JavaPlugin implements Listener {
	
	// Instantiate config variable to use globally, value set in onEnable()
    FileConfiguration config = null;
    
    // We must grab the full file path of the latest.log
    String path = System.getProperty("user.dir") + "/logs/latest.log";
    
	@Override
	public void onEnable() {
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		
		// Grab current configuration
		config = this.getConfig();

		// Add comments at the top of configuration
		config.options().header("Options for ip lookup type are: COUNTRY, COUNTRYNAME, REGIONNAME, REGIONNAME-COUNTRY, REGIONNAME-COUNTRYNAME\nTo disable join or leave messages you can set them to \"none\"");

		// Add default configuration values
		config.addDefault("QuitReason.IPLookup.Enabled", true);
		config.addDefault("QuitReason.IPLookup.Type", "REGIONNAME-COUNTRY");
		config.addDefault("QuitReason.Messages.Join", "&e{player} joined the game (&a{location}&e)");
		config.addDefault("QuitReason.Messages.Quit", "&e{player} left the game (&4{reason}&e)");

		// Save default configuration in case it does not exist
        config.options().copyHeader(true);
        config.options().copyDefaults(true);
		this.saveConfig();
		
        getLogger().info("QuitReason by Koolsami7 - Enabled");
	}
	
    @Override
    public void onDisable() {
        getLogger().info("QuitReason by Koolsami7 - Disabled");
    }
    
    // https://stackoverflow.com/a/13632114
    public static String readStringFromURL(String requestURL) throws IOException
    {
        try (Scanner scanner = new Scanner(new URL(requestURL).openStream(),
                StandardCharsets.UTF_8.toString()))
        {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
	public void onPlayerJoin(PlayerJoinEvent e) {
	    	String msg = config.getString("QuitReason.Messages.Join");
	    	if (!msg.equalsIgnoreCase("none")) {
			try {
		        Player p = e.getPlayer();
		        
		        // Replace strings
				msg = msg.replace("{player}", p.getName());
				
				// Check if IP lookup is enabled in configuration
		    		if (config.getBoolean("QuitReason.IPLookup.Enabled")) {
		    			// Grab JSON object of IP details
		    			String IPAPI = readStringFromURL("http://ip-api.com/json/"+e.getPlayer().getAddress().toString().split(":")[0]);
		    			JSONParser jp = new JSONParser();
		    			JSONObject ipobj = (JSONObject)jp.parse(IPAPI);
		    			
		    			// Grab configuration options for IP lookup
		    			String lookupType = config.getString("QuitReason.IPLookup.Type");
		    			String countryName = ipobj.get("country").toString();
		    			String countryCode = ipobj.get("countryCode").toString();
		    			String regionName = ipobj.get("regionName").toString();
		    			String regionCode = ipobj.get("region").toString();
		    			
		    			// Determine type of location from configuration
		    			switch (lookupType) {
		    			case "COUNTRY":
		    				msg = msg.replace("{location}", countryCode);
		    				break;
		    			case "COUNTRYNAME":
		    				msg = msg.replace("{location}", countryName);
		    				break;
		    			case "REGIONNAME":
		    				msg = msg.replace("{location}", regionName);
		    				break;
		    			case "REGION":
		    				msg = msg.replace("{location}", regionCode);
		    				break;
		    			case "REGIONNAME-COUNTRY":
		    				msg = msg.replace("{location}", regionName + ", " + countryCode);
		    				break;
		    			case "REGIONNAME-COUNTRYNAME":
		    				msg = msg.replace("{location}", regionName + ", " + countryName);
		    				break;
		    			default:
		    				msg = msg.replace("{location}", countryName);
		    				break;
		    			}
		    		}
		    		
		    		// Replace all & color codes with proper Minecraft color code
		    		msg = msg.replaceAll("&", ""+ChatColor.COLOR_CHAR);
		    		
		    		// Broadcast final join message 
		    		if (!msg.equalsIgnoreCase("none")) e.setJoinMessage(msg);
			} catch (Exception ex) {
				// Backup join message in-case error occurs above
				e.setJoinMessage(ChatColor.YELLOW + e.getPlayer().getName() + " joined the game");
				getLogger().log(Level.SEVERE, "Error reading IP lookup API. Using default join message.");
			}
	    	}
	}
    

    @EventHandler(priority = EventPriority.HIGH)
	public void onPlayerQuit(PlayerQuitEvent e) {
    		String msg = config.getString("QuitReason.Messages.Quit");
	    	if (!msg.equalsIgnoreCase("none")) {
	        Player p = e.getPlayer();
			try {
				File latestlog = new File(path); 
				
				// Grab last line of latest.log
				Scanner sc = new Scanner(latestlog);
				String last = "";
		        while (sc.hasNextLine()){
		            last = sc.nextLine();
		        }
		        sc.close();
		        
		        // Make sure that line has to do with our player
		        if (last.contains(p.getName()))  {
		        		String[] lastSplit = last.split(":");
		        		// Reason is always at the end, so we grab the string after the last semicolon
		        		String reason = lastSplit[4].substring(1, lastSplit[4].length());
		        		
		        		// Replace strings
		        		msg = msg.replace("{player}", p.getName());
		        		msg = msg.replace("{reason}", reason);
		        		
		        		// Replace all & color codes with proper Minecraft color code
		        		msg = msg.replaceAll("&", ""+ChatColor.COLOR_CHAR);
		        		
		        		// Broadcast final quit message
		        	    e.setQuitMessage(msg);
		        }
			} catch (Exception ex) {
				e.setQuitMessage(ChatColor.YELLOW + e.getPlayer().getName() + " left the game");
				getLogger().log(Level.SEVERE, "Error reading '" + path + "'. Using default quit message.");
			}
	    	}
	}
	
}
