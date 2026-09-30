package com.sportmate.app.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Structural guarantees of the navigation graphs that keep the main flow (including Profile)
 * behind authentication. Unit tests run with the module directory as working directory.
 */
public class NavigationGraphTest {
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private static final String APP = "http://schemas.android.com/apk/res-auto";
    private static final File NAVIGATION = new File("src/main/res/navigation");

    @Test
    public void profileIsOnlyInTheMainGraphAndReachedFromHome() throws Exception {
        assertTrue(destinationIds("main_nav_graph.xml").contains("@+id/profileFragment"));
        assertFalse(destinationIds("auth_nav_graph.xml").contains("@+id/profileFragment"));
        assertFalse(destinationIds("nav_graph.xml").contains("@+id/profileFragment"));

        Element home = destination(parse("main_nav_graph.xml"), "@+id/homeFragment");
        Element action = childAction(home, "@+id/action_home_to_profile");
        assertEquals("@id/profileFragment", action.getAttributeNS(APP, "destination"));
    }

    @Test
    public void authGraphHasNoWayIntoTheMainFlow() throws Exception {
        Set<String> authDestinations = new HashSet<>();
        for (String id : destinationIds("auth_nav_graph.xml")) {
            authDestinations.add(id.replace("@+id/", "@id/"));
        }
        NodeList actions = parse("auth_nav_graph.xml").getElementsByTagName("action");
        assertTrue(actions.getLength() > 0);
        for (int i = 0; i < actions.getLength(); i++) {
            String destination = ((Element) actions.item(i)).getAttributeNS(APP, "destination");
            assertTrue("auth action leaves the auth flow: " + destination, authDestinations.contains(destination));
        }
    }

    @Test
    public void switchingFlowsClearsTheBackStack() throws Exception {
        Document root = parse("nav_graph.xml");
        for (String[] expected : new String[][] {
                {"@+id/action_global_auth", "@id/auth_nav_graph"},
                {"@+id/action_global_main", "@id/main_nav_graph"}}) {
            Element action = childAction(root.getDocumentElement(), expected[0]);
            assertEquals(expected[1], action.getAttributeNS(APP, "destination"));
            assertEquals("@id/nav_graph", action.getAttributeNS(APP, "popUpTo"));
            assertEquals("true", action.getAttributeNS(APP, "popUpToInclusive"));
        }
    }

    @Test
    public void onlyMainActivityNavigatesIntoTheMainFlow() throws IOException {
        List<String> users;
        try (Stream<Path> files = Files.walk(new File("src/main/java").toPath())) {
            users = files.filter(p -> p.toString().endsWith(".java"))
                    .filter(NavigationGraphTest::mentionsGlobalMain)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        }
        assertEquals(Collections.singletonList("MainActivity.java"), users);
    }

    private static boolean mentionsGlobalMain(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8).contains("action_global_main");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Document parse(String file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new File(NAVIGATION, file));
    }

    private static List<String> destinationIds(String file) throws Exception {
        NodeList fragments = parse(file).getElementsByTagName("fragment");
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < fragments.getLength(); i++) {
            ids.add(((Element) fragments.item(i)).getAttributeNS(ANDROID, "id"));
        }
        return ids;
    }

    private static Element destination(Document document, String id) {
        NodeList fragments = document.getElementsByTagName("fragment");
        for (int i = 0; i < fragments.getLength(); i++) {
            Element fragment = (Element) fragments.item(i);
            if (id.equals(fragment.getAttributeNS(ANDROID, "id"))) {
                return fragment;
            }
        }
        throw new AssertionError("no destination " + id);
    }

    private static Element childAction(Element parent, String id) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element child = (Element) children.item(i);
                if ("action".equals(child.getTagName()) && id.equals(child.getAttributeNS(ANDROID, "id"))) {
                    return child;
                }
            }
        }
        throw new AssertionError("no action " + id);
    }
}
