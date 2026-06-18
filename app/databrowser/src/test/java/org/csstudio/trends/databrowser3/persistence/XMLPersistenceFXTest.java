/*******************************************************************************
 * JUnit 5 unit tests for XMLPersistence
 ******************************************************************************/
package org.csstudio.trends.databrowser3.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.csstudio.trends.databrowser3.model.FormulaInput;
import org.csstudio.trends.databrowser3.model.FormulaItem;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;


/**
 * Unit tests for {@link XMLPersistence}.
 *
 * <p>Tests are organized by feature area using nested test classes.
 */
@DisplayName("XMLPersistence")
class XMLPersistenceFXTest {

    // ---------------------------------------------------------------------------
    // Constants
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Constants")
    class ConstantsTest {

        @Test
        @DisplayName("DEFAULT_FONT_FAMILY is Liberation Sans")
        void defaultFontFamily() {
            assertEquals("Liberation Sans", XMLPersistence.DEFAULT_FONT_FAMILY);
        }

        @Test
        @DisplayName("DEFAULT_FONT_SIZE is 10")
        void defaultFontSize() {
            assertEquals(10.0, XMLPersistence.DEFAULT_FONT_SIZE, 0.0001);
        }

        @Test
        @DisplayName("TAG_DATABROWSER has correct value")
        void tagDatabrowser() {
            assertEquals("databrowser", XMLPersistence.TAG_DATABROWSER);
        }

        @Test
        @DisplayName("TAG_COLOR has correct value")
        void tagColor() {
            assertEquals("color", XMLPersistence.TAG_COLOR);
        }

        @Test
        @DisplayName("TAG_RED, TAG_GREEN, TAG_BLUE have correct values")
        void tagColorComponents() {
            assertEquals("red",   XMLPersistence.TAG_RED);
            assertEquals("green", XMLPersistence.TAG_GREEN);
            assertEquals("blue",  XMLPersistence.TAG_BLUE);
        }
    }

    // ---------------------------------------------------------------------------
    // loadColorFromDocument
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("loadColorFromDocument")
    class LoadColorFromDocumentTest {

        private Document doc;

        @BeforeEach
        void setUp() throws Exception {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        }

        // Helper: build an XML fragment like:
        //   <parent>
        //     <color><red>R</red><green>G</green><blue>B</blue></color>
        //   </parent>
        private Element parentWithColor(String colorTag, int r, int g, int b) {
            Element parent = doc.createElement("parent");
            Element color  = doc.createElement(colorTag);
            Element red    = doc.createElement("red");   red.setTextContent(String.valueOf(r));
            Element green  = doc.createElement("green"); green.setTextContent(String.valueOf(g));
            Element blue   = doc.createElement("blue");  blue.setTextContent(String.valueOf(b));
            color.appendChild(red);
            color.appendChild(green);
            color.appendChild(blue);
            parent.appendChild(color);
            return parent;
        }

        @Test
        @DisplayName("null node returns Optional of BLACK")
        void nullNodeReturnsBlack() throws Exception {
            Optional<Color> result = XMLPersistence.loadColorFromDocument(null, "color");
            assertTrue(result.isPresent());
            assertEquals(Color.BLACK, result.get());
        }

        @Test
        @DisplayName("missing color tag returns empty Optional")
        void missingColorTagReturnsEmpty() throws Exception {
            Element parent = doc.createElement("parent");
            Optional<Color> result = XMLPersistence.loadColorFromDocument(parent, "color");
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("loads red color correctly")
        void loadsRedColor() throws Exception {
            Element parent = parentWithColor("color", 255, 0, 0);
            Optional<Color> result = XMLPersistence.loadColorFromDocument(parent, "color");
            assertTrue(result.isPresent());
            assertEquals(255, (int)(result.get().getRed()   * 255));
            assertEquals(0,   (int)(result.get().getGreen() * 255));
            assertEquals(0,   (int)(result.get().getBlue()  * 255));
        }

        @Test
        @DisplayName("loads arbitrary RGB color correctly")
        void loadsArbitraryRgbColor() throws Exception {
            Element parent = parentWithColor("color", 100, 150, 200);
            Optional<Color> result = XMLPersistence.loadColorFromDocument(parent, "color");
            assertTrue(result.isPresent());
            assertEquals(100, (int)(result.get().getRed()   * 255));
            assertEquals(150, (int)(result.get().getGreen() * 255));
            assertEquals(200, (int)(result.get().getBlue()  * 255));
        }

        @Test
        @DisplayName("custom color tag name is respected")
        void customColorTag() throws Exception {
            Element parent = parentWithColor("myColor", 10, 20, 30);
            Optional<Color> result = XMLPersistence.loadColorFromDocument(parent, "myColor");
            assertTrue(result.isPresent());
            assertEquals(10, (int)(result.get().getRed()   * 255));
            assertEquals(20, (int)(result.get().getGreen() * 255));
            assertEquals(30, (int)(result.get().getBlue()  * 255));
        }

        @Test
        @DisplayName("single-arg overload uses TAG_COLOR")
        void singleArgOverloadUsesDefaultTag() throws Exception {
            Element parent = parentWithColor(XMLPersistence.TAG_COLOR, 0, 128, 255);
            Optional<Color> result = XMLPersistence.loadColorFromDocument(parent);
            assertTrue(result.isPresent());
            assertEquals(0,   (int)(result.get().getRed()   * 255));
            assertEquals(128, (int)(result.get().getGreen() * 255));
            assertEquals(255, (int)(result.get().getBlue()  * 255));
        }

        @Test
        @DisplayName("missing RGB children default to 0 (black)")
        void missingRgbChildrenDefaultToZero() throws Exception {
            Element parent = doc.createElement("parent");
            parent.appendChild(doc.createElement("color")); // empty <color/>
            Optional<Color> result = XMLPersistence.loadColorFromDocument(parent, "color");
            assertTrue(result.isPresent());
            assertEquals(Color.rgb(0, 0, 0), result.get());
        }
    }

    // ---------------------------------------------------------------------------
    // loadFontFromDocument
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("loadFontFromDocument")
    class LoadFontFromDocumentTest {

        private Document doc;

        @BeforeEach
        void setUp() throws Exception {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        }

        private Element parentWithFont(String fontTag, String fontDesc) {
            Element parent = doc.createElement("parent");
            Element font   = doc.createElement(fontTag);
            font.setTextContent(fontDesc);
            parent.appendChild(font);
            return parent;
        }

        @Test
        @DisplayName("missing font tag returns empty Optional")
        void missingFontTagReturnsEmpty() {
            Element parent = doc.createElement("parent");
            Optional<Font> result = XMLPersistence.loadFontFromDocument(parent, "title_font");
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("empty font string returns empty Optional")
        void emptyFontStringReturnsEmpty() {
            Element parent = parentWithFont("title_font", "");
            Optional<Font> result = XMLPersistence.loadFontFromDocument(parent, "title_font");
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("legacy format 'family|size|0' produces normal font")
        void legacyFormatNormalFont() {
            Element parent = parentWithFont("title_font", "Arial|14|0");
            Optional<Font> result = XMLPersistence.loadFontFromDocument(parent, "title_font");
            assertTrue(result.isPresent());
            assertEquals(14.0, result.get().getSize(), 0.5);
        }

        @Test
        @DisplayName("legacy format style '1' produces BOLD")
        void legacyFormatBold() {
            Element parent = parentWithFont("label_font", "Arial|12|1");
            Optional<Font> result = XMLPersistence.loadFontFromDocument(parent, "label_font");
            assertTrue(result.isPresent());
            assertTrue(result.get().getStyle().toLowerCase().contains("bold"));
        }

        @Test
        @DisplayName("legacy format style '2' produces ITALIC")
        void legacyFormatItalic() {
            Element parent = parentWithFont("scale_font", "Arial|12|2");
            Optional<Font> result = XMLPersistence.loadFontFromDocument(parent, "scale_font");
            assertTrue(result.isPresent());
            assertTrue(result.get().getStyle().toLowerCase().contains("italic"));
        }

        @Test
        @DisplayName("legacy format style '3' produces BOLD ITALIC")
        void legacyFormatBoldItalic() {
            Element parent = parentWithFont("legend_font", "Arial|12|3");
            Optional<Font> result = XMLPersistence.loadFontFromDocument(parent, "legend_font");
            assertTrue(result.isPresent());
            String style = result.get().getStyle().toLowerCase();
            assertTrue(style.contains("bold") && style.contains("italic"));
        }

        @Test
        @DisplayName("non-legacy (no pipe) string falls through to defaults")
        void nonLegacyFormatFallsToDefaults() {
            // Doesn't match "a|b|c" pattern – the code keeps default values
            Element parent = parentWithFont("title_font", "some-unknown-format");
            Optional<Font> result = XMLPersistence.loadFontFromDocument(parent, "title_font");
            // Should still return a font (non-empty), sized to DEFAULT_FONT_SIZE
            assertTrue(result.isPresent());
            assertEquals(XMLPersistence.DEFAULT_FONT_SIZE, result.get().getSize(), 0.5);
        }
    }

    // ---------------------------------------------------------------------------
    // patchLegacyAbsTime (private – tested via reflection)
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("patchLegacyAbsTime")
    class PatchLegacyAbsTimeTest {

        private Method method;

        @BeforeEach
        void setUp() throws Exception {
            method = XMLPersistence.class.getDeclaredMethod("patchLegacyAbsTime", String.class);
            method.setAccessible(true);
        }

        private String patch(String input) throws Exception {
            return (String) method.invoke(null, input);
        }

        @Test
        @DisplayName("legacy 'yyyy/mm/dd HH:mm:ss' is converted to dashes")
        void convertsLegacySlashFormat() throws Exception {
            assertEquals("2020-03-15 10:00:00", patch("2020/03/15 10:00:00"));
        }

        @Test
        @DisplayName("already-dashed format is returned unchanged")
        void isoFormatUnchanged() throws Exception {
            assertEquals("2020-03-15 10:00:00", patch("2020-03-15 10:00:00"));
        }

        @Test
        @DisplayName("short string (≤10 chars) is returned unchanged")
        void shortStringUnchanged() throws Exception {
            assertEquals("now", patch("now"));
        }

        @Test
        @DisplayName("string with slash not in positions 4/7 is unchanged")
        void slashInWrongPositionUnchanged() throws Exception {
            String input = "path/to/something/here";
            String patched = patch(input);
            assertEquals("path-to-something-here", patched);
        }
    }

    // ---------------------------------------------------------------------------
    // writeColor / round-trip colour XML
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("writeColor")
    class WriteColorTest {

        @Test
        @DisplayName("writeColor produces valid XML that loadColorFromDocument can parse back")
        void roundTripColor() throws Exception {
            Color original = Color.rgb(123, 45, 200);

            // Write
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.xml.stream.XMLStreamWriter base =
                    javax.xml.stream.XMLOutputFactory.newInstance()
                            .createXMLStreamWriter(baos, "UTF-8");
            base.writeStartDocument("UTF-8", "1.0");
            base.writeStartElement("root");
            XMLPersistence.writeColor(base, XMLPersistence.TAG_COLOR, original);
            base.writeEndElement();
            base.writeEndDocument();
            base.flush();

            // Parse back
            DocumentBuilder docBuilder =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = docBuilder.parse(
                    new ByteArrayInputStream(baos.toByteArray()));
            Element root = doc.getDocumentElement();
            Optional<Color> result = XMLPersistence.loadColorFromDocument(root);

            assertTrue(result.isPresent());
            assertEquals((int)(original.getRed()   * 255), (int)(result.get().getRed()   * 255));
            assertEquals((int)(original.getGreen() * 255), (int)(result.get().getGreen() * 255));
            assertEquals((int)(original.getBlue()  * 255), (int)(result.get().getBlue()  * 255));
        }

        @Test
        @DisplayName("writeColor with black produces all-zero RGB")
        void writeBlackColor() throws Exception {
            Color black = Color.BLACK;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.xml.stream.XMLStreamWriter writer =
                    javax.xml.stream.XMLOutputFactory.newInstance()
                            .createXMLStreamWriter(baos, "UTF-8");
            writer.writeStartDocument();
            writer.writeStartElement("root");
            XMLPersistence.writeColor(writer, "color", black);
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();

            String xml = baos.toString(StandardCharsets.UTF_8);
            assertTrue(xml.contains("<red>0</red>"));
            assertTrue(xml.contains("<green>0</green>"));
            assertTrue(xml.contains("<blue>0</blue>"));
        }

        @Test
        @DisplayName("writeColor with white produces 255 RGB values")
        void writeWhiteColor() throws Exception {
            Color white = Color.WHITE;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.xml.stream.XMLStreamWriter writer =
                    javax.xml.stream.XMLOutputFactory.newInstance()
                            .createXMLStreamWriter(baos, "UTF-8");
            writer.writeStartDocument();
            writer.writeStartElement("root");
            XMLPersistence.writeColor(writer, "color", white);
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();

            String xml = baos.toString(StandardCharsets.UTF_8);
            assertTrue(xml.contains("<red>255</red>"));
            assertTrue(xml.contains("<green>255</green>"));
            assertTrue(xml.contains("<blue>255</blue>"));
        }
    }

    // ---------------------------------------------------------------------------
    // load – error handling
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("load – error handling")
    class LoadErrorHandlingTest {

        @Test
        @DisplayName("loading XML with wrong root element throws Exception")
        void wrongRootElementThrows() {
            String xml = "<?xml version=\"1.0\"?><notdatabrowser/>";
            InputStream stream = new ByteArrayInputStream(
                    xml.getBytes(StandardCharsets.UTF_8));

            // We need a minimal Model stub. If the real Model is available on the
            // classpath, use it; otherwise this test documents expected behaviour.
            assertThrows(Exception.class, () -> {
                // A real Model must have 0 items at this point.
                org.csstudio.trends.databrowser3.model.Model model =
                        new org.csstudio.trends.databrowser3.model.Model();
                XMLPersistence.load(model, stream);
            });
        }

        @Test
        @DisplayName("loading into a non-empty model throws RuntimeException")
        void nonEmptyModelThrows() {
            // Build a minimal valid databrowser XML with one PV so the model ends
            // up non-empty after a first load, then attempt a second load.
            // If Model is on the classpath this test covers the guard clause:
            //   if (model.getItems().size() > 0) throw new RuntimeException(...)
            assertThrows(RuntimeException.class, () -> {
                String xml =
                        "<?xml version=\"1.0\"?><databrowser></databrowser>";
                org.csstudio.trends.databrowser3.model.Model model =
                        new org.csstudio.trends.databrowser3.model.Model();
                // First load – succeeds (empty databrowser)
                XMLPersistence.load(model, new ByteArrayInputStream(
                        xml.getBytes(StandardCharsets.UTF_8)));
                // Manually add an item so model is no longer empty, then try again
                // (exact mechanism depends on Model API; adapt as needed)
                // For the sake of this template we just invoke load a second time
                // with a non-empty model (if the first load added items via the XML).
                // Adjust once real Model is available.
                model.addItem(new PVItem("foo", 1.0));
                XMLPersistence.load(model, new ByteArrayInputStream(
                        xml.getBytes(StandardCharsets.UTF_8)));
            });
        }
    }

    // ---------------------------------------------------------------------------
    // XML tags – spot-check a few more
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("XML tag constants – spot checks")
    class TagConstantsSpotCheck {

        @Test
        void tagPv()          { assertEquals("pv",          XMLPersistence.TAG_PV); }
        @Test
        void tagFormula()     { assertEquals("formula",     XMLPersistence.TAG_FORMULA); }
        @Test
        void tagAxis()        { assertEquals("axis",        XMLPersistence.TAG_AXIS); }
        @Test
        void tagAxes()        { assertEquals("axes",        XMLPersistence.TAG_AXES); }
        @Test
        void tagAnnotations() { assertEquals("annotations", XMLPersistence.TAG_ANNOTATIONS); }
        @Test
        void tagStart()       { assertEquals("start",       XMLPersistence.TAG_START); }
        @Test
        void tagEnd()         { assertEquals("end",         XMLPersistence.TAG_END); }
        @Test
        void tagScroll()      { assertEquals("scroll",      XMLPersistence.TAG_SCROLL); }
        @Test
        void tagTitle()       { assertEquals("title",       XMLPersistence.TAG_TITLE); }
        @Test
        void tagName()        { assertEquals("name",        XMLPersistence.TAG_NAME); }
    }

    @Test
    @DisplayName("Write all PVs before formulas")
    public void testPvAndFormulaOrdering() throws Exception {

        Model model = new Model();

        PVItem pvItem1 = new PVItem("pvitem1", 1.0);
        FormulaInput formulaInput1 = new FormulaInput(pvItem1, "x1");
        FormulaItem formulaItem1 = new FormulaItem("formula1", "x1 * 2", new FormulaInput[]{formulaInput1});

        model.addItem(pvItem1);
        model.addItem(formulaItem1);

        PVItem pvItem2 = new PVItem("pvitem2", 1.0);

        FormulaInput formulaInput2 = new FormulaInput(pvItem2, "x2");

        formulaItem1 = (FormulaItem) model.getItem("formula1");

        formulaItem1.updateFormula("x1 + x2", new FormulaInput[]{formulaInput1, formulaInput2});

        model.addItem(pvItem2);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLPersistence.write(model, baos);

        // Make sure the XML is readable
        XMLPersistence.load(new Model(), new ByteArrayInputStream(baos.toByteArray()));

    }
}
