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
package fr.lixbox.service.registry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import fr.lixbox.io.json.JsonUtil;


@WebServlet(urlPatterns="/configuration")
public class ConfigurationServlet extends HttpServlet 
{
    private static final long serialVersionUID = 3009650591589313586L;
    private static final Log LOG = LogFactory.getLog(ConfigurationServlet.class);
    
    
    @ConfigProperty(name="registry.api.url")     
    String registryApi;
    @ConfigProperty(name="iam.api.url") 
    String iamApi;

    

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    {
        Map<String,String> config = new HashMap<>();
        config.put("registry", registryApi);
        config.put("iam", iamApi);
        try
        {
            response.getWriter().print(JsonUtil.transformObjectToJson(config,false));
        }
        catch (IOException e)
        {
            LOG.fatal(e,e);
        }
    }
}