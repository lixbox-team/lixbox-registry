/*******************************************************************************
 *    
 *                           FRAMEWORK Lixbox
 *                          ==================
 *      
 * This file is part of lixbox-service registry.
 *
 *    lixbox-iam is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    lixbox-iam is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *    along with lixbox-service registry.  If not, see <https://www.gnu.org/licenses/>
 *   
 *   @AUTHOR Lixbox-team
 *
 ******************************************************************************/
package fr.lixbox.service.registry.redis;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
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

import com.fasterxml.jackson.core.type.TypeReference;

import fr.lixbox.common.exceptions.ProcessusException;
import fr.lixbox.common.util.CodeVersionUtil;
import fr.lixbox.common.util.CollectionUtil;
import fr.lixbox.common.util.StringUtil;
import fr.lixbox.io.json.JsonUtil;
import fr.lixbox.service.common.model.Instance;
import fr.lixbox.service.common.util.ServiceUtil;
import fr.lixbox.service.registry.RegistryService;
import fr.lixbox.service.registry.model.ServiceEntry;
import fr.lixbox.service.registry.model.ServiceType;
import fr.lixbox.service.registry.model.health.Check;
import fr.lixbox.service.registry.model.health.ServiceState;
import fr.lixbox.service.registry.model.health.ServiceStatus;
import redis.clients.jedis.Jedis;

/**
 * Ce registre de service fonctionne sur le file system.
 * 
 * @author ludovic.terral
 */
@ApplicationScoped
@Path(RegistryService.SERVICE_URI)
@Produces({"application/json"})
@Consumes({"application/json"})
public class RedisRegistryServiceBean implements RegistryService
{
    private static final String SERVICE_REDIS_TEXT = "LE SERVICE REDIS ";
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
    public ServiceState checkHealth() 
    {
        return checkReady();
    }

    
    
    @Override
    public ServiceState checkReady()
    {
        LOG.info("redis uri: "+redisUri);
        LOG.debug("Check Health started");
        ServiceState state = new ServiceState();
        
        //controle de redis
        if (!StringUtil.isEmpty(redisUri))
        { 
            String hostName = redisUri.substring(6,redisUri.lastIndexOf(':'));
            String port = redisUri.substring(redisUri.lastIndexOf(':')+1);
            try (
                Jedis redisClient = new Jedis(hostName, Integer.parseInt(port));
            )
            {           
                redisClient.ping();
                state.setStatus(ServiceStatus.UP);
                LOG.debug(SERVICE_REDIS_TEXT+redisUri+" EST DISPONIBLE");
            }
            catch (Exception e)
            {
                LOG.fatal(e,e);
                LOG.error(SERVICE_REDIS_TEXT+redisUri+" N'EST PAS DISPONIBLE");
                state.setStatus(ServiceStatus.DOWN);
                state.getChecks().add(new Check(ServiceStatus.DOWN, SERVICE_REDIS_TEXT+redisUri+" N'EST PAS DISPONIBLE"));
            }
        }
        else
        {
            state.setStatus(ServiceStatus.DOWN);
            state.getChecks().add(new Check(ServiceStatus.DOWN, "IMPOSSIBLE DE TROUVER LE SERVICE REDIS"));
        }
        LOG.debug("Check Health finished");
        if (state.getStatus().equals(ServiceStatus.DOWN))
        {
            throw new ProcessusException(state.toString());
        }
        return state;
    }
    
    
    
    @Override public ServiceState checkLive() 
    {
        return new ServiceState(ServiceStatus.UP);
    }



    @Override
    public String getVersion()
    {        
        return CodeVersionUtil.getVersion(this.getClass());
    }



    @Override
    public boolean registerService(String name,  String version, ServiceType type,  String uri)
    {
        boolean result = false;
        try(
            Jedis redisClient = getRedisClient();
        )        
        {  
            if (redisClient==null || StringUtil.isEmpty(name) || StringUtil.isEmpty(version) || StringUtil.isEmpty(uri))
            {
                return result;
            }       
            ServiceEntry serviceEntry = JsonUtil.transformJsonToObject(redisClient.get(HEAD_KEYS+name+":"+version), new TypeReference<ServiceEntry>(){});
            if (serviceEntry == null)
            {
                serviceEntry = new ServiceEntry();
                serviceEntry.setName(name);
                serviceEntry.setVersion(version);
                serviceEntry.setType(type);
            }
            if (!serviceEntry.containsInstanceUri(uri))
            {
                serviceEntry.getInstances().add(new Instance(uri));
                redisClient.set(HEAD_KEYS+name+":"+version, JsonUtil.transformObjectToJson(serviceEntry,false));       
            }            
            result = true;
        }
        catch (Exception e)
        {
            LOG.error(e);
            LOG.debug(e.getMessage());
        }
        return result;
    }



    @Override
    public boolean unregisterService(String name, String version, String uri)
    {  
        boolean result = false;
        try(
            Jedis redisClient = getRedisClient();
        )        
        {  
            if (redisClient==null || StringUtil.isEmpty(name) || StringUtil.isEmpty(version) || StringUtil.isEmpty(uri))
            {
                return result;
            }       
            ServiceEntry serviceEntry = JsonUtil.transformJsonToObject(redisClient.get(HEAD_KEYS+name+":"+version), new TypeReference<ServiceEntry>(){});
            if (serviceEntry!=null)
            {
                if (serviceEntry.containsInstanceUri(uri))
                {
                    serviceEntry.getInstances().remove(serviceEntry.getInstanceByUri(uri));            
                }                
                if (serviceEntry.getInstances().isEmpty())
                {
                    redisClient.del(HEAD_KEYS+name+":"+version);
                }
                else
                {            
                    redisClient.set(HEAD_KEYS+name+":"+version, serviceEntry.toString());   
                }
                result = true;
            }
        }
        catch (Exception e)
        {
            LOG.error(e);
            LOG.debug(e.getMessage());
        }
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
                result = JsonUtil.transformJsonToObject(redisClient.get(HEAD_KEYS+name+":"+version), new TypeReference<ServiceEntry>(){});         
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
		        entries.add(JsonUtil.transformJsonToObject(redisClient.get(key), new TypeReference<ServiceEntry>(){}));
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
                entries.add(JsonUtil.transformJsonToObject(redisClient.get(key), new TypeReference<ServiceEntry>(){}));
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
        try(            
            Jedis redisClient = getRedisClient();
        )
        {     
            if (redisClient==null || entry==null)
            {
                return entry;
            }       
            checkEntry(entry);
            redisClient.set(HEAD_KEYS+entry.getName()+":"+entry.getVersion(), JsonUtil.transformObjectToJson(entry,false));
        }
        catch (Exception e)
        {
            LOG.error(e);
            LOG.debug(e.getMessage());
        }
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
            ServiceEntry serviceEntry = JsonUtil.transformJsonToObject(redisClient.get(HEAD_KEYS+name+":"+version), new TypeReference<ServiceEntry>(){});
            if (serviceEntry != null)
            {
                result = redisClient.del(HEAD_KEYS+name+":"+version, JsonUtil.transformObjectToJson(serviceEntry, false))>0;
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
    
    
    private void checkEntry(ServiceEntry entry)
    {
        for (Instance servInst:entry.getInstances())
        {
            servInst.setLiveState(ServiceUtil.checkHealth(entry.getType(), servInst.getUri()));
            servInst.setReadyState(ServiceUtil.checkHealth(entry.getType(), servInst.getUri()));
            servInst.setReady(ServiceStatus.UP.equals(servInst.getReadyState().getStatus()));
            servInst.setLive(ServiceStatus.UP.equals(servInst.getLiveState().getStatus()));
        }
    }
}