package org.gatein.wcm.impl.jcr;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.gatein.wcm.api.model.publishing.PublishStatus;
import org.gatein.wcm.api.model.security.ACE;
import org.gatein.wcm.api.model.security.ACE.PermissionType;
import org.gatein.wcm.api.model.security.ACL;
import org.gatein.wcm.api.model.security.Principal;
import org.gatein.wcm.api.model.security.Principal.PrincipalType;
import org.gatein.wcm.api.model.security.User;
import org.gatein.wcm.api.services.exceptions.ContentException;
import org.gatein.wcm.api.services.exceptions.ContentIOException;
import org.gatein.wcm.api.services.exceptions.ContentSecurityException;
import org.gatein.wcm.impl.model.WCMConstants;
import org.gatein.wcm.impl.model.WCMContentFactory;
import org.jboss.logging.Logger;

/**
 *
 * All JCR low level operations should be placed here.
 *
 * @author lucas
 *
 */
public class JcrMappings {

    private static final Logger log = Logger.getLogger("org.gatein.wcm.jcr");

    private final String MARK = "__";

    WCMContentFactory factory = null;

    Session jcrSession = null;
    User logged = null;
    VersionManager vm = null;

    public JcrMappings(Session session, User user)
            throws ContentIOException
    {
        try {
            jcrSession = session;
            logged = user;
            vm = jcrSession.getWorkspace().getVersionManager();
        } catch (RepositoryException e) {
            throw new ContentIOException ("Unexpected error initializating session JCR objects. Msg: " + e.getMessage());
        }
    }

    public WCMContentFactory getFactory() {
        return factory;
    }

    public void setFactory(WCMContentFactory factory) {
        this.factory = factory;
    }

    public boolean checkSession() {
        if (this.jcrSession == null || this.logged == null)
            return false;
        return true;
    }

    public boolean checkLocation(String location) {

        if (location == null) return false;
        if (location.equals("/")) return true;

        try {
            return jcrSession.nodeExists(location);
        } catch (RepositoryException e) {
            log.error("Location " + location + " bad specified. Message: " + e.getMessage());
        }
        return false;
    }

    public boolean checkLocation(String location, String locale) {

        if (location == null) return false;
        if (location.equals("/")) return true;

        try {
            return jcrSession.nodeExists(location + "/" + MARK + locale);
        } catch (RepositoryException e) {
            log.error("Location " + location + " bad specified. Message: " + e.getMessage());
        }
        return false;
    }


    public boolean checkIdExists(String location, String id, String locale) {
        try {
            Node root = jcrSession.getNode(location + "/" + id + "/" + MARK + locale);
            if (root.getPrimaryNodeType().getName().equals("nt:folder"))
                return true;
        } catch (PathNotFoundException e) {
            return false;
        } catch (RepositoryException e) {
            log.error("Unexpected error in location " + location + "/" + id + "/" + MARK + locale + ". Message: " + e.getMessage());
        }
        return false;
    }

    public boolean checkIdExists(String location, String id) {
        try {
            Node root = jcrSession.getNode(location + "/" + id);
            if (root.getPrimaryNodeType().getName().equals("nt:folder"))
                return true;
        } catch (PathNotFoundException e) {
            return false;
        } catch (RepositoryException e) {
            log.error("Unexpected error in location " + location + "/" + id + ". Message: " + e.getMessage());
        }
        return false;
    }

    public boolean checkUserWriteACL(String location) {

        // Create ACL from location
        ACL acl = null;
        try {
            // Check if we are in the root node or child node
            Node n = null;
            if (location.equals("/")) {
                n = jcrSession.getNode("/__acl");
            } else {
                n = jcrSession.getNode(location + "/" + MARK + "acl");
            }

            String __acl = n.getNode("jcr:content").getProperty("jcr:data").getString();
            acl = factory.parseACL(location, "ACL for " + location, __acl);

        } catch (PathNotFoundException e) {
            // If there are not __acl folder in the location, we will check to the parent node
            if ( ! location.equals("/") ) {
                return checkUserWriteACL( parent(location) );
            } else {
                // If root node has not __acl folder means that there are not security in this repository
                return true;
            }
        } catch (RepositoryException e) {
            log.error("Unexpected error looking for acl in location " + location + ". Msg: " + e.getMessage());
            return false;
        }

        // Validate ACL with logged user
        for (ACE ace : acl.getAces()) {
            // Check if we have a GROUP ACE
            if ( ace.getPrincipal().getType() == PrincipalType.GROUP &&
                 Arrays.asList( PermissionType.WRITE, PermissionType.ALL ).contains( ace.getPermission() )) {
                for ( String group : logged.getGroups() )
                    if ( group.equals( ace.getPrincipal().getId() ) )
                        return true;
            }
            // Check if we have a USER ACE
            if ( ace.getPrincipal().getType() == PrincipalType.USER &&
                 ace.getPrincipal().getId().equals( logged.getUserName() ) &&
                 Arrays.asList( PermissionType.WRITE, PermissionType.ALL ).contains( ace.getPermission() ) )
                return true;
        }

        return false;
    }

    public boolean checkUserReadACL(String location) {

        // Create ACL from location
        ACL acl = null;
        try {
            // Check if we are in the root node or child node
            Node n = null;
            if (location.equals("/")) {
                n = jcrSession.getNode("/__acl");
            } else {
                n = jcrSession.getNode(location + "/" + MARK + "acl");
            }

            String __acl = n.getNode("jcr:content").getProperty("jcr:data").getString();
            acl = factory.parseACL(location, "ACL for " + location, __acl);

        } catch (PathNotFoundException e) {
            // If there are not __acl folder in the location, we will check to the parent node
            if ( ! location.equals("/") ) {
                return checkUserReadACL( parent(location) );
            } else {
                // If root node has not __acl folder means that there are not security in this repository
                return true;
            }
        } catch (RepositoryException e) {
            log.error("Unexpected error looking for acl in location " + location + ". Msg: " + e.getMessage());
            return false;
        }

        // Validate ACL with logged user
        for (ACE ace : acl.getAces()) {
            // Check if we have a GROUP ACE
            if ( ace.getPrincipal().getType() == PrincipalType.GROUP &&
                 Arrays.asList( PermissionType.READ, PermissionType.COMMENTS, PermissionType.WRITE, PermissionType.ALL ).contains( ace.getPermission() )) {
                for ( String group : logged.getGroups() )
                    if ( group.equals( ace.getPrincipal().getId() ) )
                        return true;
            }
            // Check if we have a USER ACE
            if ( ace.getPrincipal().getType() == PrincipalType.USER &&
                 ace.getPrincipal().getId().equals( logged.getUserName() ) &&
                 Arrays.asList( PermissionType.READ, PermissionType.COMMENTS, PermissionType.WRITE, PermissionType.ALL ).contains( ace.getPermission() ) )
                return true;
        }

        return false;
    }

    public void checkJCRException(RepositoryException e)
        throws ContentException, ContentIOException, ContentSecurityException
    {
        if (e instanceof PathNotFoundException) {
            throw new ContentException("Location doesn't found. Msg: " + e.getMessage());
        }
        if (e instanceof ItemExistsException) {
            throw new ContentException("Item exists. Msg: " + e.getMessage());
        }
        if (e instanceof NoSuchNodeTypeException) {
            throw new ContentException("Trying to write in a different node type. Msg: " + e.getMessage());
        }
        if (e instanceof LockException) {
            throw new ContentSecurityException("Trying to write in a lock node. Msg: " + e.getMessage());
        }
        if (e instanceof VersionException) {
            throw new ContentSecurityException("Error in versioning. Msg: " + e.getMessage());
        }
        if (e instanceof ConstraintViolationException) {
            throw new ContentSecurityException("Unexpected constraint violation. Msg: " + e.getMessage());
        }
        if (e instanceof ValueFormatException) {
            throw new ContentException("Wrong value format. Msg: " + e.getMessage());
        }
        if (e instanceof AccessDeniedException) {
            throw new ContentSecurityException("Access denied. Msg: " + e.getMessage());
        }
        if (e instanceof ReferentialIntegrityException) {
            throw new ContentException("Unexpected referencial integrity. Msg: " + e.getMessage());
        }
        throw new ContentIOException("Unexpected repository error. Msg: " + e.getMessage());
    }

    public boolean checkLocaleContent(String location)
    {
        try {
            Node n = jcrSession.getNode( location );

            String description = null;
            try {
                if (n.getProperty("jcr:description") != null)
                    description = n.getProperty("jcr:description").getString();
            } catch (PathNotFoundException e) {
                // This node has not mix:title, so exception ignored
            }

            // Only TextContent or BinaryContent has locales
            if (description != null
                && ( "textcontent".contains( description ) ||
                      "binarycontent".contains( description )))
            {
                return true;
            }
        } catch (Exception e) {
            log.error("Unexpected error looking for locales. Msg: " + e.getMessage());
        }
        return false;
    }

    public void createTextNode(String id, String locale, String location, Value content, String encoding)
        throws RepositoryException
    {
        if (! checkIdExists(location, id)) {
            jcrSession.getNode(location).addNode(id, "nt:folder");
        }

        jcrSession.getNode(location + "/" + id)
            .addNode(MARK + locale, "nt:folder")
            .addNode(MARK + id, "nt:file")
            .addNode("jcr:content", "nt:resource");

        Node n = jcrSession.getNode(location + "/" + id);
        n.addMixin("mix:title");
        n.addMixin("mix:lastModified");
        n.setProperty("jcr:description", "textcontent");

        n = jcrSession.getNode(location + "/" + id + "/" + MARK + locale + "/" + MARK + id);
        n.addMixin("mix:title");
        n.addMixin("mix:versionable");
        n.addMixin("mix:lastModified");
        n.addMixin("mix:mimeType");

        // Adding properties
        n.getNode("jcr:content").setProperty("jcr:data", content);
        n.setProperty("jcr:description", content.getString());
        n.setProperty("jcr:encoding", encoding);

        // Saving changes into JCR
        jcrSession.save();
    }

    public String deleteNode(String location)
        throws RepositoryException
    {
        jcrSession.removeItem(location);

        // Saving changes into JCR
        jcrSession.save();

        return parent(location);
    }

    public String deleteNode(String location, String locale)
            throws RepositoryException
        {
            jcrSession.removeItem(location + "/" + MARK + locale);

            // Check if we have a textcontent or binarycontent orphan of locales, then we delete

            boolean orphan = true;
            Node n = jcrSession.getNode( location );
            NodeIterator ni = n.getNodes();
            while (ni.hasNext()) {
                Node child = ni.nextNode();
                String name = child.getName();
                if (! WCMConstants.RESERVED_ENTRIES.contains( name )) {
                    if (name.startsWith("__")) orphan = false;
                }
            }

            // Saving changes into JCR
            jcrSession.save();

            if (orphan) {
                return deleteNode(location);
            } else {
                // We still have locales under same location, so we return same node instead parent
                return location;
            }
        }


    public void createFolder(String id, String location)
        throws RepositoryException
    {
        Node n = jcrSession.getNode( location ).addNode(id, "nt:folder");
        n.addMixin("mix:title");
        n.addMixin("mix:versionable");
        n.addMixin("mix:lastModified");

        n.setProperty("jcr:description", "folder");

        // Saving changes into JCR
        jcrSession.save();
    }

    public void createBinaryNode(String id, String locale, String location, String contentType, Long size,
            String fileName, InputStream content)
        throws RepositoryException
    {
        if (! checkIdExists(location, id)) {
            jcrSession.getNode(location).addNode(id, "nt:folder");
        }

        jcrSession.getNode(location + "/" + id)
            .addNode(MARK + locale, "nt:folder")
            .addNode(MARK + id, "nt:file")
            .addNode("jcr:content", "nt:resource");

        Node n = jcrSession.getNode(location + "/" + id);
        n.addMixin("mix:title");
        n.addMixin("mix:lastModified");

        n.setProperty("jcr:description", "binarycontent");

        n = jcrSession.getNode(location + "/" + id + "/" + MARK + locale + "/" + MARK + id);
        n.addMixin("mix:title");
        n.addMixin("mix:versionable");
        n.addMixin("mix:lastModified");
        n.addMixin("mix:mimeType");

        // Adding properties

        Binary _content = jcrSession.getValueFactory().createBinary(content);

        n.getNode("jcr:content").setProperty("jcr:data", _content);
        n.setProperty("jcr:title", fileName);
        n.setProperty("jcr:mimeType", contentType);
        n.setProperty("jcr:description", size);

        // Saving changes into JCR
        jcrSession.save();
    }

    // Read methods

    public Session getSession() {
        return this.jcrSession;
    }

    public List<String> getLocales(String location)
        throws RepositoryException
    {
        Node n = jcrSession.getNode( location );

        ArrayList<String> locales = new ArrayList<String>();

        NodeIterator ni = n.getNodes();
        while (ni.hasNext()) {
            Node child = ni.nextNode();
            String name = child.getName();
            if (! WCMConstants.RESERVED_ENTRIES.contains( name ) ) {
                if (name.startsWith( "__" )) locales.add( name.substring( 2 ) );
            }
        }

        if (locales.isEmpty())
            return null;

        return locales;
    }

    public void updateTextNode(String location, String locale, Value content, String encoding)
        throws RepositoryException
    {
        if ("/".equals( location )) return;

        String id = location.substring( location.lastIndexOf("/") + 1);

        Node n = jcrSession.getNode(location + "/" + MARK + id);

        // In TextNodes we store the html also in jcr:description for future search funtionality
        n.setProperty("jcr:description", content);
        n.getNode("jcr:content").setProperty("jcr:data", content);

        jcrSession.save();
    }

    public void updateFolderLocation(String location, String newLocation)
       throws RepositoryException
    {
        // Root node is not affected
        if ("/".equals( location )) return;

        jcrSession.move(location, newLocation);

        jcrSession.save();
    }

    public void updateFolderName(String location, String newName)
        throws RepositoryException
    {
        // Root node is not affected
        if ("/".equals( location )) return;

        Node n = jcrSession.getNode( location );

        jcrSession.move(location, n.getParent().getPath() + "/" + newName);

        jcrSession.save();
    }

    public void updateBinaryNode(String location, String locale, String contentType, Long size,
            String fileName, InputStream content)
        throws RepositoryException
    {
        String id = location.substring( location.lastIndexOf("/") + 1 );

        Node n = jcrSession.getNode(location + "/" + MARK + locale + "/" + MARK + id);

        Binary _content = jcrSession.getValueFactory().createBinary(content);

        n.getNode("jcr:content").setProperty("jcr:data", _content);
        n.setProperty("jcr:title", fileName);
        n.setProperty("jcr:mimeType", contentType);
        n.setProperty("jcr:description", size);

        jcrSession.save();
    }


    // JCR Aux methods
    public Value jcrValue(String content, String encoding)
        throws RepositoryException
    {
        try {
            return jcrSession.getValueFactory().createValue( new String(content.getBytes(encoding), encoding) );
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException("Bad encoding : " + encoding);
        }
    }

    public Integer jcrVersion(String location) {
        try {
            VersionHistory h = vm.getVersionHistory( location );
            return new Integer( (int) h.getAllLinearFrozenNodes().getSize() );
        } catch (Exception e) {
            log.error( "Unexpected error getting version history of " + location + ". Msg: " + e.getMessage() );
            return 0;
        }
    }

    public Integer jcrVersion(Node n) {
        try {
            return jcrVersion( n.getPath() );
        } catch (Exception e) {
            log.error( "Unexpected error getting version history of " + n.toString() + ". Msg: " + e.getMessage() );
            return 0;
        }
    }

    public Date jcrCreated(String location) {
        try {
            return jcrSession.getNode( location ).getProperty( "jcr:created" ).getDate().getTime();
        } catch (Exception e) {
            log.error( "Unexpected error getting created date for " + location + ". Msg: " + e.getMessage() );
            return null;
        }
    }

    public Date jcrCreated(Node n) {
        try {
            return n.getProperty( "jcr:created" ).getDate().getTime();
        } catch (Exception e) {
            log.error( "Unexpected error getting created date for " + n.toString() + ". Msg: " + e.getMessage() );
            return null;
        }
    }

    public Date jcrLastModified(String location) {
        try {
            return jcrSession.getNode( location ).getProperty( "jcr:lastModified" ).getDate().getTime();
        } catch (Exception e) {
            log.error( "Unexpected error getting created date for " + location + ". Msg: " + e.getMessage() );
            return null;
        }
    }

    public Date jcrLastModified(Node n) {
        try {
            return n.getProperty( "jcr:lastModified" ).getDate().getTime();
        } catch (Exception e) {
            log.error( "Unexpected error getting created date for " + n.toString() + ". Msg: " + e.getMessage() );
            return null;
        }
    }


    public ACL jcrACL(String location) {

        // Create ACL from location
        ACL acl = null;
        try {
            // Check if we are in the root node or child node
            Node n = null;
            if (location.equals("/")) {
                n = jcrSession.getNode("/__acl");
            } else {
                n = jcrSession.getNode(location + "/" + MARK + "acl");
            }

            String __acl = n.getNode("jcr:content").getProperty("jcr:data").getString();
            acl = factory.parseACL(location, "ACL for " + location, __acl);
            return acl;

        } catch (PathNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            log.error("Unexpected error looking for acl in location " + location + ". Msg: " + e.getMessage());
            return null;
        }
    }

    public ACL jcrACL(Node n) {

        // Create ACL from location
        ACL acl = null;
        try {
            // Check if we are in the root node or child node

            String __acl = n.getNode("jcr:content").getProperty("jcr:data").getString();
            acl = factory.parseACL(n.getPath(), "ACL for " + n.getPath(), __acl);
            return acl;

        } catch (PathNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            log.error("Unexpected error looking for acl in location " + n.toString() + ". Msg: " + e.getMessage());
            return null;
        }
    }

    public PublishStatus jcrPublishStatus(String location) {
        // TODO to complete
        return null;
    }

    public PublishStatus jcrPublishStatus(Node n) {
        // TODO to complete
        return null;
    }

    public List<Principal> jcrPublishingRoles(String location) {
        // TODO to complete
        return null;
    }

    public List<Principal> jcrPublishingRoles(Node n) {
        // TODO to complete
        return null;
    }

    public String jcrCreatedBy(String location) {
        try {
            Node n = jcrSession.getNode( location );
            return n.getProperty("jcr:createdBy").getString();
        } catch (RepositoryException e) {
            log.error("Unexpected error retrieving jcr:createdBy user for location " + location + ". Msg: " + e.getMessage());
            return null;
        }
    }

    public String jcrCreatedBy(Node n) {
        try {
            return n.getProperty("jcr:createdBy").getString();
        } catch (RepositoryException e) {
            log.error("Unexpected error retrieving jcr:createdBy user for location " + n.toString() + ". Msg: " + e.getMessage());
            return null;
        }
    }

    public String jcrLastModifiedBy(String location) {
        try {
            Node n = jcrSession.getNode( location );
            return n.getProperty("jcr:lastModifiedBy").getString();
        } catch (RepositoryException e) {
            log.error("Unexpected error retrieving jcr:lastModifiedBy user for location " + location + ". Msg: " + e.getMessage());
            return null;
        }
    }

    public String jcrLastModifiedBy(Node n) {
        try {
            return n.getProperty("jcr:lastModifiedBy").getString();
        } catch (RepositoryException e) {
            log.error("Unexpected error retrieving jcr:lastModifiedBy user for location " + n.toString() + ". Msg: " + e.getMessage());
            return null;
        }
    }

    public String jcrEncoding(Node n) {
        try {
            return n.getProperty("jcr:encoding").getString();
        } catch (RepositoryException e) {
            log.error("Unexpected error retrieving jcr:encoding user for location " + n.toString() + ". Msg: " + e.getMessage());
            return null;
        }
    }

    public String jcrTextContent(Node n) {
        try {
            return n.getNode("jcr:content").getProperty("jcr:data").getString();
        } catch (RepositoryException e) {
            log.error("Unexpected error retrieving jcr:data user for location " + n.toString() + ". Msg: " + e.getMessage());
            return null;
        }
    }

    public String jcrContentType(Node n) {
        try {
            return n.getProperty("jcr:mimeType").getString();
        } catch (RepositoryException e) {
            log.error("Unexpected error retrieving jcr:mimeType user for location " + n.toString() + ". Msg: " + e.getMessage());
            return null;
        }
    }

    public InputStream jcrContent(Node n) {
        try {
            return n.getNode("jcr:content").getProperty("jcr:data").getBinary().getStream();
        } catch (RepositoryException e) {
            log.error("Unexpected error retrieving jcr:content user for location " + n.toString() + ". Msg: " + e.getMessage());
            return null;
        }
    }

    public byte[] jcrContentData(Node n) {
        try {
            return toByteArray(n.getNode("jcr:content").getProperty("jcr:data").getBinary().getStream());
        } catch (RepositoryException e) {
            log.error("Unexpected error retrieving jcr:content user for location " + n.toString() + ". Msg: " + e.getMessage());
            return null;
        }
    }

    public String jcrDescription(Node n) {
        try {
            return n.getProperty("jcr:description").getString();
        } catch (RepositoryException e) {
            log.error("Unexpected error retrieving jcr:description user for location " + n.toString() + ". Msg: " + e.getMessage());
            return null;
        }
    }

    public String jcrTitle(Node n) {
        try {
            return n.getProperty("jcr:title").getString();
        } catch (RepositoryException e) {
            log.error("Unexpected error retrieving jcr:title user for location " + n.toString() + ". Msg: " + e.getMessage());
            return null;
        }
    }


    // Aux methods
    public String parent(String location) {

        if (location == null) return null;

        if ("/".equals( location )) return location;

        String[] locs = location.split("/");

        if (locs.length > 2) {
            StringBuffer sb = new StringBuffer( location.length() );
            for (int i=1; i < (locs.length -1); i++) {
                sb.append("/" + locs[i]);
            }
            return sb.toString();
        } else {
            return "/";
        }
    }

    public byte[] toByteArray(InputStream is) {
        try {

            byte[] data = new byte[16000];
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            return buffer.toByteArray();
        } catch (Exception e) {
            log.error("Error creating createBinaryContent() transforming toByteArray(). Msg: " + e.getMessage());
        }
        return null;
    }




}