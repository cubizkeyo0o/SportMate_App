package com.sportmate.app.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sportmate.app.presentation.booking.BookCourtViewModel;
import com.sportmate.app.presentation.booking.BookingDetailViewModel;
import com.sportmate.app.presentation.booking.BookingSummaryViewModel;
import com.sportmate.app.presentation.courts.CourtDetailViewModel;

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
    public void homeLeadsToCourtsAndCourtsToDetailWithARequiredCourtId() throws Exception {
        for (String graph : new String[] {"auth_nav_graph.xml", "nav_graph.xml"}) {
            assertFalse(destinationIds(graph).contains("@+id/courtsFragment"));
            assertFalse(destinationIds(graph).contains("@+id/courtDetailFragment"));
        }
        Document main = parse("main_nav_graph.xml");
        assertEquals("@id/courtsFragment",
                childAction(destination(main, "@+id/homeFragment"), "@+id/action_home_to_courts")
                        .getAttributeNS(APP, "destination"));
        assertEquals("@id/courtDetailFragment",
                childAction(destination(main, "@+id/courtsFragment"), "@+id/action_courts_to_court_detail")
                        .getAttributeNS(APP, "destination"));

        Element argument = childElement(destination(main, "@+id/courtDetailFragment"), "argument");
        assertEquals(CourtDetailViewModel.ARG_COURT_ID, argument.getAttributeNS(ANDROID, "name"));
        assertEquals("string", argument.getAttributeNS(APP, "argType"));
        // Required: no default and not nullable.
        assertFalse(argument.hasAttributeNS(ANDROID, "defaultValue"));
        assertFalse("true".equals(argument.getAttributeNS(APP, "nullable")));
    }

    @Test
    public void bookingFlowPassesIdsExplicitly() throws Exception {
        for (String graph : new String[] {"auth_nav_graph.xml", "nav_graph.xml"}) {
            for (String id : new String[] {"@+id/bookCourtFragment", "@+id/bookingSummaryFragment",
                    "@+id/bookingDetailFragment", "@+id/myBookingsFragment"}) {
                assertFalse(destinationIds(graph).contains(id));
            }
        }
        Document main = parse("main_nav_graph.xml");
        assertAction(main, "@+id/courtDetailFragment", "@+id/action_court_detail_to_book_court", "@id/bookCourtFragment");
        assertAction(main, "@+id/bookCourtFragment", "@+id/action_book_court_to_summary", "@id/bookingSummaryFragment");
        assertAction(main, "@+id/bookingSummaryFragment", "@+id/action_summary_to_booking_detail", "@id/bookingDetailFragment");
        assertAction(main, "@+id/homeFragment", "@+id/action_home_to_my_bookings", "@id/myBookingsFragment");
        assertAction(main, "@+id/myBookingsFragment", "@+id/action_my_bookings_to_booking_detail", "@id/bookingDetailFragment");

        assertRequiredStrings(destination(main, "@+id/bookCourtFragment"), BookCourtViewModel.ARG_COURT_ID);
        assertRequiredStrings(destination(main, "@+id/bookingSummaryFragment"),
                BookingSummaryViewModel.ARG_COURT_ID, BookingSummaryViewModel.ARG_COURT_SPORT_ID,
                BookingSummaryViewModel.ARG_DATE, BookingSummaryViewModel.ARG_START_TIME, BookingSummaryViewModel.ARG_END_TIME);
        assertRequiredStrings(destination(main, "@+id/bookingDetailFragment"), BookingDetailViewModel.ARG_BOOKING_ID);
    }

    @Test
    public void createdBookingReplacesTheSummaryOnTheBackStack() throws Exception {
        Element toDetail = childAction(destination(parse("main_nav_graph.xml"), "@+id/bookingSummaryFragment"),
                "@+id/action_summary_to_booking_detail");

        // Popping to the court removes Book Court and Summary: Back cannot reach "Confirm booking" again.
        assertEquals("@id/courtDetailFragment", toDetail.getAttributeNS(APP, "popUpTo"));
        assertFalse("true".equals(toDetail.getAttributeNS(APP, "popUpToInclusive")));
    }

    @Test
    public void discoveryNavigationKeepsTheBackStack() throws Exception {
        Document main = parse("main_nav_graph.xml");
        for (Element action : new Element[] {
                childAction(destination(main, "@+id/homeFragment"), "@+id/action_home_to_courts"),
                childAction(destination(main, "@+id/courtsFragment"), "@+id/action_courts_to_court_detail")}) {
            // Without popUpTo, Back returns Court Detail -> Courts -> Home.
            assertFalse(action.hasAttributeNS(APP, "popUpTo"));
            assertFalse(action.hasAttributeNS(APP, "popUpToInclusive"));
        }
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

    private static void assertAction(Document graph, String from, String action, String to) {
        assertEquals(to, childAction(destination(graph, from), action).getAttributeNS(APP, "destination"));
    }

    // Each named argument exists as a required (no default, not nullable) string.
    private static void assertRequiredStrings(Element destination, String... names) {
        NodeList children = destination.getElementsByTagName("argument");
        for (String name : names) {
            Element found = null;
            for (int i = 0; i < children.getLength(); i++) {
                Element argument = (Element) children.item(i);
                if (name.equals(argument.getAttributeNS(ANDROID, "name"))) {
                    found = argument;
                }
            }
            if (found == null) {
                throw new AssertionError("missing argument " + name);
            }
            assertEquals("string", found.getAttributeNS(APP, "argType"));
            assertFalse(found.hasAttributeNS(ANDROID, "defaultValue"));
            assertFalse("true".equals(found.getAttributeNS(APP, "nullable")));
        }
    }

    private static Element childElement(Element parent, String tag) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element && tag.equals(((Element) children.item(i)).getTagName())) {
                return (Element) children.item(i);
            }
        }
        throw new AssertionError("no " + tag);
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
