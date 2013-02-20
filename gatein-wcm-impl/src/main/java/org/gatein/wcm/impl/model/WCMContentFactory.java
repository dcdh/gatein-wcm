package org.gatein.wcm.impl.model;

import java.io.InputStream;
import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.gatein.wcm.api.model.content.Content;
import org.gatein.wcm.api.model.security.ACE.PermissionType;
import org.gatein.wcm.api.model.security.ACL;
import org.gatein.wcm.api.model.security.Principal;
import org.gatein.wcm.api.model.security.User;
import org.gatein.wcm.api.services.exceptions.ContentIOException;
import org.gatein.wcm.impl.jcr.JcrMappings;
import org.jboss.logging.Logger;

/**
 *
 * All org.gatein.wcm.model factory methods should be placed here.
 *
 * @author lucas
 *
 */
public class WCMContentFactory {

    private static final Logger log = Logger.getLogger("org.gatein.wcm.model");

    User logged = null;
    JcrMappings jcr = null;

    private final String MARK = "__";

    public WCMContentFactory(JcrMappings jcr, User user)
        throws ContentIOException
    {
        logged = user;
        this.jcr = jcr;
    }

    public Content createTextContent(String id, String locale, String location, String html,
            String encoding) {

        WCMTextContent c = new WCMTextContent();

        if ("/".equals( location ) ) location = "";

        String absLocation = location + "/" + id + "/" + MARK + locale + "/" + MARK + id;

        // New document, so new version starting at 1
        c.setVersion( jcr.jcrVersion( absLocation  ) );
        c.setId( id );
        c.setLocale( locale );
        c.setLocation( location );

        // By default a new content will get the ACL of parent parent.
        // A null value means that this content is using ACL of parent folder.
        c.setAcl( null );

        c.setCreated( jcr.jcrCreated( absLocation ) );
        c.setLastModified( jcr.jcrLastModified( absLocation ) );

        // By default a new content will get the Publishing status of his parent
        // A null value means that this content is using parent's publishing information
        c.setPublishStatus( null );
        c.setPublishingRoles( null );

        c.setCreatedBy( logged );
        c.setLastModifiedBy( logged );

        // By default a new content will not use attached

        c.setLocked( false );

        c.setLockOwner( null );

        // Specific fields for TextContent
        c.setContent( html );
        c.setEncoding( encoding );

        return c;
    }

    /**
     *
     * GateIn WCM represents a ACL list a String stored into a file called "__acl" in the content location.
     * This file is a String with the following structure:
     *
     *  user:[USER|GROUP]:[NONE|READ|COMMENTS|WRITE|ALL],user:[USER|GROUP]:[NONE|READ|COMMENTS|WRITE|ALL], ...
     *
     * @param str
     * @return
     */
    public ACL parseACL(String id, String description, String acl) {
        WCMACL wcmACL = new WCMACL(id, description);
        String[] aces = acl.split(",");
        for (String ace : aces) {
            String user = ace.split(":")[0];
            String type = ace.split(":")[1];
            String permission = ace .split(":")[2];

            WCMPrincipal wcmPrincipal = null;
            WCMACE wcmACE = null;
            if (type.equals("USER"))
                wcmPrincipal = new WCMPrincipal(user, Principal.PrincipalType.USER);
            else
                wcmPrincipal = new WCMPrincipal(user, Principal.PrincipalType.GROUP);

            if (permission.equals("NONE")) {
                wcmACE = new WCMACE(wcmPrincipal, PermissionType.NONE);
            }
            if (permission.equals("READ")) {
                wcmACE = new WCMACE(wcmPrincipal, PermissionType.READ);
            }
            if (permission.equals("COMMENTS")) {
                wcmACE = new WCMACE(wcmPrincipal, PermissionType.COMMENTS);
            }
            if (permission.equals("WRITE")) {
                wcmACE = new WCMACE(wcmPrincipal, PermissionType.WRITE);
            }
            if (permission.equals("ALL")) {
                wcmACE = new WCMACE(wcmPrincipal, PermissionType.ALL);
            }
            wcmACL.getAces().add( wcmACE );
        }
        return wcmACL;
    }

    public Content createFolder(String id, String location) {

        WCMFolder f = new WCMFolder();

        String absLocation = location + "/" + id ;

        if ("/".equals( location ) ) location = "";

        // New document, so new version starting at 1
        f.setVersion( jcr.jcrVersion( absLocation  ) );
        f.setId( id );
        // Folders can have multiple locales, so, it will be null.
        f.setLocale( null );
        f.setLocation( location );

        // By default a new content will get the ACL of parent parent.
        // A null value means that this content is using ACL of parent folder.
        f.setAcl( null );

        f.setCreated( jcr.jcrCreated( absLocation ) );
        f.setLastModified( jcr.jcrLastModified( absLocation ) );

        // By default a new content will get the Publishing status of his parent
        // A null value means that this content is using parent's publishing information
        f.setPublishStatus( null );
        f.setPublishingRoles( null );

        f.setCreatedBy( logged );
        f.setLastModifiedBy( logged );

        // By default a new content will not use attached

        f.setLocked( false );

        f.setLockOwner( null );

        // Specific fields for Folder
        // New node, so no children at this point
        f.setChildren( null );

        return f;
    }

    public Content createBinaryContent(String id, String locale, String location, String contentType, Long size,
            String fileName, InputStream content) {

        WCMBinaryContent b = new WCMBinaryContent();

        if ("/".equals( location ) ) location = "";

        String absLocation = location + "/" + id + "/" + MARK + locale + "/" + MARK + id;

        // New document, so new version starting at 1
        b.setVersion( jcr.jcrVersion( absLocation  ) );
        b.setId( id );
        b.setLocale( locale );
        b.setLocation( location );

        // By default a new content will get the ACL of parent parent.
        // A null value means that this content is using ACL of parent folder.
        b.setAcl( null );

        b.setCreated( jcr.jcrCreated( absLocation ) );
        b.setLastModified( jcr.jcrLastModified( absLocation ) );

        // By default a new content will get the Publishing status of his parent
        // A null value means that this content is using parent's publishing information
        b.setPublishStatus( null );
        b.setPublishingRoles( null );

        b.setCreatedBy( logged );
        b.setLastModifiedBy( logged );

        // By default a new content will not use attached

        b.setLocked( false );

        b.setLockOwner( null );

        // Specific fields for TextContent
        b.setFileName( fileName );
        b.setSize( size );
        b.setContentType( contentType );

        // Creating the in memory
        // Point to improve in the future

        b.setContent( content );

        return b;
    }

    public Content getContent(String location, String locale) throws RepositoryException
    {
        // Get root node of search
        Node n = jcr.getSession().getNode( location );

        Content c = convertToContent( n, locale );

        if (c instanceof WCMFolder) {
            WCMFolder f = (WCMFolder)c;
            ArrayList<Content> children = new ArrayList<Content>();
            f.setChildren( children );
            NodeIterator ni = n.getNodes();
            while (ni.hasNext()) {
                Node child = ni.nextNode();
                Content cChild = getContent(child.getPath(), locale);
                if (cChild != null)
                    children.add( cChild );
            }
        }

        return c;
    }

    private Content convertToContent(Node n, String locale) throws RepositoryException {

        // Check if we are using some reserved entries in the JCR
        if (n == null || locale == null) return null;

        if (WCMConstants.RESERVED_ENTRIES.contains( n.getName() )) {
            return null;
        }

        // We have a folder if we don't have any "__*" sub-folder representing locale.
        // We discard specials folders:
        //  __acl -> for __acl
        //  __wcmstatus -> for Publishing status
        //  __wcmroles -> for Publishing roles
        //  __comments -> for Comments
        //  __categories -> for Categories
        //  __properties -> for Properties
        boolean root = false;
        boolean folder = false;
        boolean textcontent = false;
        boolean binarycontent = false;
        boolean havelocale = false;

        if ("/".equals( n.getPath() ))
            root = true;

        String description = null;

        try {
            if (n.getProperty("jcr:description") != null)
                description = n.getProperty("jcr:description").getString();
        } catch (PathNotFoundException e) {
            // This node has not mix:title, so exception ignored
        }

        if (description != null && "folder".equals( description ))
            folder = true;

        if (description != null && "textcontent".contains( description ))
            textcontent = true;

        if (description != null && "binarycontent".contains( description ))
            binarycontent = true;

        // Check if the content has the proper locale

        if (textcontent || binarycontent) {
            try {
                n.getNode(MARK + locale + "/" + MARK + n.getName());
            } catch (PathNotFoundException e) {
                return null;
            }
        }

        // Look and convert node to content

        if (root) {
            WCMFolder _folder = new WCMFolder();

            _folder.setVersion( 0 ); // Special for root
            _folder.setId( "root" );
            // Folders can have multiple locales, so, it will be null.
            _folder.setLocale( null );
            _folder.setLocation( "/" );

            // By default a new content will get the ACL of parent parent.
            // A null value means that this content is using ACL of parent folder.
            _folder.setAcl( jcr.jcrACL( n ) );

            _folder.setCreated( null );
            _folder.setLastModified( null );

            // By default a new content will get the Publishing status of his parent
            // A null value means that this content is using parent's publishing information
            _folder.setPublishStatus( jcr.jcrPublishStatus( n ) );
            _folder.setPublishingRoles( jcr.jcrPublishingRoles( n ) );

            _folder.setCreatedBy( null );
            _folder.setLastModifiedBy( null );

            // By default a folder will not be locked
            // TODO: Set up in future
            _folder.setLocked( false );

            _folder.setLockOwner( null );

            // Specific fields for Folder
            // New node, so no children at this point
            _folder.setChildren( null );

            return _folder;
        }
        if (folder) {
            WCMFolder _folder = new WCMFolder();

            _folder.setVersion( jcr.jcrVersion( n ) );
            _folder.setId( n.getName() );
            // Folders can have multiple locales, so, it will be null.
            _folder.setLocale( null );
            _folder.setLocation( jcr.parent(n.getPath()) );

            // By default a new content will get the ACL of parent parent.
            // A null value means that this content is using ACL of parent folder.
            _folder.setAcl( jcr.jcrACL( n ) );

            _folder.setCreated( jcr.jcrCreated( n ) );
            _folder.setLastModified( jcr.jcrLastModified( n ) );

            // By default a new content will get the Publishing status of his parent
            // A null value means that this content is using parent's publishing information
            _folder.setPublishStatus( jcr.jcrPublishStatus( n ) );
            _folder.setPublishingRoles( jcr.jcrPublishingRoles( n ) );

            _folder.setCreatedBy( new WCMUser(jcr.jcrCreatedBy( n ) ) );
            _folder.setLastModifiedBy( new WCMUser(jcr.jcrLastModifiedBy( n )) );

            // By default a folder will not be locked
            // TODO: Set up in future
            _folder.setLocked( false );

            _folder.setLockOwner( null );

            // Specific fields for Folder
            // New node, so no children at this point
            _folder.setChildren( null );

            return _folder;
        }
        if (textcontent) {
            WCMTextContent _textcontent = new WCMTextContent();

            _textcontent.setVersion( jcr.jcrVersion( n.getNode(MARK + locale + "/" + MARK + n.getName() ) ) );
            _textcontent.setId( n.getName() );
            // Folders can have multiple locales, so, it will be null.
            _textcontent.setLocale( locale );
            _textcontent.setLocation( jcr.parent(n.getPath()) );

            // By default a new content will get the ACL of parent parent.
            // A null value means that this content is using ACL of parent folder.
            _textcontent.setAcl( jcr.jcrACL( n ) );

            _textcontent.setCreated( jcr.jcrCreated( n.getNode(MARK + locale + "/" + MARK + n.getName() ) ) );
            _textcontent.setLastModified( jcr.jcrLastModified( n.getNode(MARK + locale + "/" + MARK + n.getName() ) ) );

            // By default a new content will get the Publishing status of his parent
            // A null value means that this content is using parent's publishing information
            _textcontent.setPublishStatus( jcr.jcrPublishStatus( n ) );
            _textcontent.setPublishingRoles( jcr.jcrPublishingRoles( n ) );

            _textcontent.setCreatedBy( new WCMUser(jcr.jcrCreatedBy( n.getNode(MARK + locale + "/" + MARK + n.getName() ) ) )  );
            _textcontent.setLastModifiedBy( new WCMUser(jcr.jcrLastModifiedBy( n.getNode(MARK + locale + "/" + MARK + n.getName() ) ) ) );

            // By default a folder will not be locked
            // TODO: Set up in future
            _textcontent.setLocked( false );

            _textcontent.setLockOwner( null );

            _textcontent.setEncoding( jcr.jcrEncoding( n.getNode(MARK + locale + "/" + MARK + n.getName() ) ) );

            _textcontent.setContent( jcr.jcrTextContent( n.getNode(MARK + locale + "/" + MARK + n.getName() ) ) );

            return _textcontent;
        }
        if (binarycontent) {
            WCMBinaryContent _binarycontent = new WCMBinaryContent();

            _binarycontent.setVersion( jcr.jcrVersion( n.getNode(MARK + locale + "/" + MARK + n.getName() ) ) );
            _binarycontent.setId( n.getName() );
            // Folders can have multiple locales, so, it will be null.
            _binarycontent.setLocale( locale );
            _binarycontent.setLocation( jcr.parent(n.getPath()) );

            // By default a new content will get the ACL of parent parent.
            // A null value means that this content is using ACL of parent folder.
            _binarycontent.setAcl( jcr.jcrACL( n ) );

            _binarycontent.setCreated( jcr.jcrCreated( n.getNode(MARK + locale + "/" + MARK + n.getName() ) ) );
            _binarycontent.setLastModified( jcr.jcrLastModified( n.getNode(MARK + locale + "/" + MARK + n.getName() ) ) );

            // By default a new content will get the Publishing status of his parent
            // A null value means that this content is using parent's publishing information
            _binarycontent.setPublishStatus( jcr.jcrPublishStatus( n ) );
            _binarycontent.setPublishingRoles( jcr.jcrPublishingRoles( n ) );

            _binarycontent.setCreatedBy( new WCMUser(jcr.jcrCreatedBy( n.getNode(MARK + locale + "/" + MARK + n.getName() ) )) );
            _binarycontent.setLastModifiedBy( new WCMUser(jcr.jcrLastModifiedBy( n.getNode(MARK + locale + "/" + MARK + n.getName() ) )) );

            // By default a folder will not be locked
            // TODO: Set up in future
            _binarycontent.setLocked( false );

            _binarycontent.setLockOwner( null );

            _binarycontent.setContentType( jcr.jcrContentType( n.getNode(MARK + locale + "/" + MARK + n.getName() ) ) );

            _binarycontent.setFileName( jcr.jcrTitle( n.getNode(MARK + locale + "/" + MARK + n.getName() ) ) );
            _binarycontent.setContent( jcr.jcrContent( n.getNode(MARK + locale + "/" + MARK + n.getName() ) ) );
            return _binarycontent;
        }
        if (havelocale) {
            log.info("Found node: " + n.getPath() + " but without locale: " + locale);
        }

        return null;
    }


}