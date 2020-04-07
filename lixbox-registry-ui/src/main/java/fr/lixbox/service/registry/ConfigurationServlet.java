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
package fr.lixbox.service.registry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import fr.lixbox.io.json.JsonUtil;


@WebServlet(urlPatterns="/configuration")
public class ConfigurationServlet extends HttpServlet 
{
    private static final long serialVersionUID = 3009650591589313586L;
    
    
    @ConfigProperty(name="registry.api.url")     
    private String registryApi;
    @ConfigProperty(name="iam.api.url") 
    private String iamApi;

    

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException 
    {
        Map<String,String> config = new HashMap<>();
        config.put("registry", registryApi);
        config.put("iam", iamApi);
        response.getWriter().print(JsonUtil.transformObjectToJson(config,false));
    }
}