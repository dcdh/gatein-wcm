package org.gatein.wcm.impl.tests;

import java.io.File;

import javax.annotation.Resource;

import junit.framework.Assert;

import org.gatein.wcm.api.services.ContentService;
import org.gatein.wcm.api.services.RepositoryService;
import org.gatein.wcm.api.services.exceptions.ContentSecurityException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class Test001_Connectivity {

    private static final Logger log = Logger.getLogger("org.gatein.wcm.impl.tests.test001");

    @Deployment
    public static Archive<?> createDeployment() {

        return ShrinkWrap.create(WebArchive.class, "gatein-wcm-impl-test001.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
    }

    @Resource(mappedName = "java:jboss/gatein-wcm")
    RepositoryService repos;

    @Test
    public void good_password() {

        log.info("[[ START TEST  good_password ]]");
        try {
            ContentService cs = repos.createContentSession("sample", "default", "admin", "admin");
            log.info(cs);
        } catch (Exception e) {
            log.error(e.getMessage());
            Assert.assertTrue(false);
        }
        Assert.assertTrue(true);
        log.info("[[ END TEST  good_password ]]");
    }

    @Test
    public void bad_password() {

        log.info("[[ START TEST  bad_password ]]");
        try {
            ContentService cs = repos.createContentSession("sample", "default", "admin", "badpassword");
            log.info(cs);
            log.error("Expecting exception...");
            Assert.assertTrue(false);
        } catch (ContentSecurityException e) {
            log.info(" [[ Expected: " + e.getMessage());
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
        Assert.assertTrue(true);
        log.info("[[ END TEST  bad_password ]]");
    }

}
