/*******************************************************************************
 *    
 *                           FRAMEWORK Lixbox
 *                          ==================
 *      
 *   Copyrigth - LIXTEC - Tous droits reserves.
 *   
 *   Le contenu de ce fichier est la propriete de la societe Lixtec.
 *   
 *   Toute utilisation de ce fichier et des informations, sous n'importe quelle
 *   forme necessite un accord ecrit explicite des auteurs
 *   
 *   @AUTHOR Ludovic TERRAL
 *
 ******************************************************************************/
package fr.lixbox.service.registry.redis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.lixbox.common.exceptions.ProcessusException;
import fr.lixbox.common.util.CodeVersionUtil;
import fr.lixbox.common.util.CollectionUtil;
import fr.lixbox.common.util.StringUtil;
import fr.lixbox.io.json.JsonUtil;
import fr.lixbox.service.registry.RegistryService;
import fr.lixbox.service.registry.model.ServiceEntry;
import redis.clients.jedis.Jedis;

/**
 * Ce registre de service fonctionne sur le file system.
 * 
 * @author ludovic.terral
 */
@RequestScoped
@Path(RegistryService.SERVICE_URI)
@Produces({"application/json"})
@Consumes({"application/json"})
public class RedisRegistryServiceBean implements RegistryService
{
    // ----------- Attribut(s) -----------   
    private static final long serialVersionUID = 1201703311339L;
    private static final Log LOG = LogFactory.getLog(RegistryService.class);
    private static final String HEAD_KEYS = "REGISTRY-SERVICE:";
    
    @ConfigProperty(name="registry.redis.uri") private String redisUri;

    
    
    // ----------- Methode(s) -----------
    public RedisRegistryServiceBean()
    {
        //a voir
    }



    @Override
    public boolean checkHealth()
    {
        LOG.info("redis uri: "+redisUri);
        LOG.debug("Check Health started");
        boolean health = true;
        Map<String,String> status = new HashMap<>();
        
        
        //controle de redis
        Jedis redisClient=null;
        try
        {
            if (!StringUtil.isEmpty(redisUri))
            {            
                String hostName = redisUri.substring(6,redisUri.lastIndexOf(':'));
                String port = redisUri.substring(redisUri.lastIndexOf(':')+1);
                redisClient = new Jedis(hostName, Integer.parseInt(port));
                redisClient.ping();
                redisClient.close();
                LOG.debug("LE SERVICE REDIS "+redisUri+" EST DISPONIBLE");
            }
            else
            {
                throw new ProcessusException("IMPOSSIBLE DE TROUVER LE SERVICE REDIS");
            }
        }
        catch (Exception e)
        {
            LOG.fatal(e,e);
            LOG.error("LE SERVICE REDIS "+redisUri+" N'EST PAS DISPONIBLE");
            status.put("REDIS", "LE SERVICE REDIS "+redisUri+" N'EST PAS DISPONIBLE");
            if (redisClient!=null && redisClient.isConnected())
            {
                redisClient.close();
            }
            health=false;
        }
        finally {
            if (redisClient!=null && redisClient.isConnected())
            {
                redisClient.close();
            }
        }
        
        LOG.debug("Check Health finished");
        if (!health)
        {
            throw new ProcessusException("LE SERVICE REGISTRY EST INDISPONIBLE. "+ JsonUtil.transformObjectToJson(status, true));
        }
        return health;
    }



    @Override
    public String getVersion()
    {        
        return CodeVersionUtil.getVersion(this.getClass());
    }



    @Override
    public boolean registerService(String name,  String version,  String uri)
    {
        Jedis redisClient = getRedisClient();
        boolean result = false;
        if (redisClient==null || StringUtil.isEmpty(name) || StringUtil.isEmpty(version) || StringUtil.isEmpty(uri))
        {
            return result;
        }       
        
        try
        {        
            ServiceEntry serviceEntry = jsonToServiceEntry(redisClient.get(HEAD_KEYS+name+":"+version));
            if (serviceEntry == null)
            {
                serviceEntry = new ServiceEntry();
                serviceEntry.setName(name);
                serviceEntry.setVersion(version);
            }
            if (!serviceEntry.getUris().contains(uri))
            {
                serviceEntry.getUris().add(uri);
                redisClient.set(HEAD_KEYS+name+":"+version, serviceEntryToJson(serviceEntry));       
                
            }            
            result = true;
        }
        catch (Exception e)
        {
            LOG.error(e);
            LOG.debug(e.getMessage());
        }
        redisClient.close();
        return result;
    }



    @Override
    public boolean unregisterService(String name, String version, String uri)
    {  
        Jedis redisClient = getRedisClient();
        boolean result = false;
        if (redisClient==null || StringUtil.isEmpty(name) || StringUtil.isEmpty(version) || StringUtil.isEmpty(uri))
        {
            return result;
        }       
        
        try
        {      
            ServiceEntry serviceEntry = jsonToServiceEntry(redisClient.get(HEAD_KEYS+name+":"+version));
            if (serviceEntry!=null)
            {
                if (!CollectionUtil.isEmpty(serviceEntry.getUris()) && serviceEntry.getUris().contains(uri))
                {
                    serviceEntry.getUris().remove(uri);            
                }                
                if (serviceEntry.getUris().isEmpty())
                {
                    redisClient.del(HEAD_KEYS+name+":"+version);
                }
                else
                {            
                    redisClient.set(HEAD_KEYS+name+":"+version, serviceEntryToJson(serviceEntry));   
                }
                result = true;
            }
        }
        catch (Exception e)
        {
            LOG.error(e);
            LOG.debug(e.getMessage());
        }
        redisClient.close();
        return result;
    }


    
    @Override
    public ServiceEntry discoverService(String name, String version)
    {

        Jedis redisClient = getRedisClient();
        ServiceEntry result = null;
        if (redisClient==null || StringUtil.isEmpty(name) || StringUtil.isEmpty(version))
        {
            return result;
        }
        
        try
        {      
            if (!CollectionUtil.isEmpty(redisClient.keys(HEAD_KEYS+name+":"+version)))
            {
                result = jsonToServiceEntry(redisClient.get(HEAD_KEYS+name+":"+version));         
            }
            else
            {
                LOG.debug("Service " + name.replace("\n", "") + " on version " + version.replace("\n", "")  + " not found");
            }
        }
        catch (Exception e)
        {
            LOG.error(e);
            LOG.debug(e.getMessage());
        }
        redisClient.close();
        return result;
    }



	@Override
	public List<ServiceEntry> getEntries(String name) 
	{
        Jedis redisClient = getRedisClient();
		List<ServiceEntry> entries = new ArrayList<>();
		if(redisClient==null || StringUtil.isEmpty(name))
		{
			return entries;
		}
	
		try 
		{
		    for(String key : redisClient.keys((HEAD_KEYS+name+"*")))
		    {
		        entries.add(jsonToServiceEntry(redisClient.get(key)));
		    }
		}
        catch (Exception e)
        {
            LOG.error(e);
            LOG.debug(e.getMessage());
        }		
        if (CollectionUtil.isEmpty(entries))
        {
            LOG.debug("Service " + name.replace("\n", "") + " not found");
        }
        redisClient.close();
		return entries;
	}



	@Override
	public List<ServiceEntry> getEntries() 
	{
        Jedis redisClient = getRedisClient();
	    List<ServiceEntry> entries = new ArrayList<>();
	    if (redisClient==null)
	    {
	        return entries;
	    }
        try 
        {
            for(String key : redisClient.keys((HEAD_KEYS+"*")))
            {
                entries.add(jsonToServiceEntry(redisClient.get(key)));
            }
        }
        catch (Exception e)
        {
            LOG.error(e);
            LOG.debug(e.getMessage());
        }    
        redisClient.close();
		return entries;
	}
	
	
	@GET
    @Path("/entries/keys")
	public List<String> getEntryKeys()
	{
	    List<String> entryKeys = new ArrayList<>();
	    for (ServiceEntry entry : getEntries())
        {
            entryKeys.add(entry.getName()+":"+entry.getVersion());
        }
	    return entryKeys;
	}

	
	
    @POST
    @Path("/entry/sync")
    public ServiceEntry synchronizeEntry(ServiceEntry entry) 
    {
        Jedis redisClient = getRedisClient();
        if (redisClient==null || entry==null)
        {
            return entry;
        }       
        
        try
        {        
            redisClient.set(HEAD_KEYS+entry.getName()+":"+entry.getVersion(), serviceEntryToJson(entry));
        }
        catch (Exception e)
        {
            LOG.error(e);
            LOG.debug(e.getMessage());
        }
        redisClient.close();
        return entry;
    }

    
    
    @DELETE
    @Path("/entry/{name}/{version}")
    public boolean removeEntry(@PathParam("name") String name, @PathParam("version") String version) 
    {
        Jedis redisClient = getRedisClient();
        boolean result = false;
        if (redisClient==null || StringUtil.isEmpty(name) || StringUtil.isEmpty(version))
        {
            return result;
        }       
        
        try
        {        
            ServiceEntry serviceEntry = jsonToServiceEntry(redisClient.get(HEAD_KEYS+name+":"+version));
            if (serviceEntry != null)
            {
                result = redisClient.del(HEAD_KEYS+name+":"+version, serviceEntryToJson(serviceEntry))>0;
            }            
        }
        catch (Exception e)
        {
            LOG.error(e);
            LOG.debug(e.getMessage());
        }
        redisClient.close();
        return result;
    }
    
    
    private ServiceEntry jsonToServiceEntry(String content) throws IOException
    {
        ServiceEntry result = null;
        ObjectMapper mapper = new ObjectMapper();
        if (!StringUtil.isEmpty(content))
        {
            try 
            {
                result = mapper.readValue(content, ServiceEntry.class);
            } 
            catch (JsonProcessingException e) 
            {
                LOG.error(e);
                LOG.debug(e.getMessage());
            }
        }
        return result;
    }

    
    
    private String serviceEntryToJson(ServiceEntry content) throws IOException
    {
        String result = "Content error";
        ObjectMapper mapper = new ObjectMapper();
        try 
        {
            result = mapper.writeValueAsString(content);
        } 
        catch (JsonProcessingException e) 
        {
            LOG.error(e);
        }
        return result;
    }
    
    
    
    private Jedis getRedisClient()
    {     
        Jedis redisClient = null;
        if (!StringUtil.isEmpty(redisUri))
        {            
            String hostName = redisUri.substring(6,redisUri.lastIndexOf(':'));
            String port = redisUri.substring(redisUri.lastIndexOf(':')+1);
            redisClient = new Jedis(hostName, Integer.parseInt(port));
            LOG.debug("redisClient is activated");
        }
        return redisClient;
    }    
}