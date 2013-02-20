package org.gatein.wcm.impl.services.commands;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.gatein.wcm.api.model.security.User;
import org.gatein.wcm.api.services.exceptions.ContentException;
import org.gatein.wcm.api.services.exceptions.ContentIOException;
import org.gatein.wcm.api.services.exceptions.ContentSecurityException;
import org.gatein.wcm.impl.jcr.JcrMappings;
import org.gatein.wcm.impl.model.WCMContentFactory;
import org.jboss.logging.Logger;

public class DeleteCommand {

    private static final Logger log = Logger.getLogger("org.gatein.wcm.commands");

    Session jcrSession = null;
    User logged = null;
    WCMContentFactory factory = null;
    JcrMappings jcr = null;

    public DeleteCommand (Session session, User user)
            throws ContentIOException
        {
            jcrSession = session;
            logged = user;
            jcr = new JcrMappings(jcrSession, logged);
            factory = new WCMContentFactory(jcr, logged);
            jcr.setFactory( factory );
        }

    /**
     *
     * Removes content from a specified location. <br>
     * All locales and versions are removed. <br>
     *
     * @param location - Location where the content is stored. <br>
     *        String with format: / &lt;id&gt; / &lt;id&gt; / &lt;id&gt; <br>
     *        where "/" is the root of repository and &lt;id&gt; folders ID
     * @return Parent location of the removed content
     * @throws ContentException if content doesn't exist
     * @throws ContentIOException if any IO related problem with repository.
     * @throws ContentSecurityException if user has not been granted to modify content under specified location.
     */
    public String deleteContent(String location) throws ContentException, ContentIOException, ContentSecurityException {

        log.debug("deleteContent()");

        checkNullParameters(location);

        // Check if the current JCR Session is valid
        if ( ! jcr.checkSession() )
            throw new ContentIOException("JCR Session is null");

        if ("/".equals( location ))
            throw new ContentException("Root location cannot be deteled by API");

        // Check if the location specified exists in the JCR Repository/Workspace
        if ( ! jcr.checkLocation(location) )
            throw new ContentException("Location: " + location + " doesn't exist for deleteContet() operation. ");

        // Delete a node
        try {
            String parent = jcr.deleteNode( location );
            return parent;
        } catch(RepositoryException e) {
            jcr.checkJCRException(e);
        }
        return null;
    }

    private void checkNullParameters(String location) throws ContentException
    {
        if (location == null || "".equals( location )) {
            new ContentException("Parameter location cannot be null or empty");
        }
    }

    /**
     * Deletes content from a specified location. <br>
     *
     * @param location - Location where the content is stored. <br>
     *        String with format: / &lt;id&gt; / &lt;id&gt; / &lt;id&gt; <br>
     *        where "/" is the root of repository and &lt;id&gt; folders ID
     * @param locale - Locale version of the content to remove.
     * @return Current location of the content, or parent location if all locales were removed.
     * @throws ContentException if content doesn't exist
     * @throws ContentIOException if any IO related problem with repository.
     * @throws ContentSecurityException if user has not been granted to modify content under specified location.
     */
   public String deleteContent(String location, String locale) throws ContentException, ContentIOException, ContentSecurityException {

       log.debug("deleteContent()");

       checkNullParameters(location, locale);

       // Check if the current JCR Session is valid
       if ( ! jcr.checkSession() )
           throw new ContentIOException("JCR Session is null");

       if ("/".equals( location ))
           throw new ContentException("Root location cannot be deteled by API");

       // Check if the location specified exists in the JCR Repository/Workspace
       if ( ! jcr.checkLocation(location, locale) )
           throw new ContentException("Location: " + location + " with locale: " + locale + " doesn't exist for deleteContent() operation. ");

       // If we try to delete a folder we invoke the generic deleteContent() method
       if ( ! jcr.checkLocaleContent( location )) {
           return deleteContent( location );
       }

       // Delete a node
       try {
           String parent = jcr.deleteNode( location, locale );
           return parent;
       } catch(RepositoryException e) {
           jcr.checkJCRException(e);
       }
       return null;
   }

   private void checkNullParameters(String location, String locale) throws ContentException
   {
       if (location == null || "".equals( location )) {
           new ContentException("Parameter location cannot be null or empty");
       }
       if (locale == null || "".equals( locale )) {
           new ContentException("Parameter locale cannot be null or empty");
       }
   }


}