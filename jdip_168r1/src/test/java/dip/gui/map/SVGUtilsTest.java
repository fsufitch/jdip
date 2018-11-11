package dip.gui.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SVGUtilsTest {

    @Test
    public void appendToStringBuffer() {
        StringBuffer sb = new StringBuffer();


        SVGUtils.appendFloat(sb.delete(0, sb.length()), 1.0f);
        assertEquals("1", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), 1.1f);
        assertEquals("1.1", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), 1.11f);
        assertEquals("1.1", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), 1.111f);
        assertEquals("1.1", sb.toString());


        SVGUtils.appendFloat(sb.delete(0, sb.length()), 5.0f);
        assertEquals("5", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), 5.5f);
        assertEquals("5.5", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), 5.55f);
        assertEquals("5.6", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), 5.555f);
        assertEquals("5.6", sb.toString());


        SVGUtils.appendFloat(sb.delete(0, sb.length()), 15.0f);
        assertEquals("15", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), 15.5f);
        assertEquals("15.5", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), 15.55f);
        assertEquals("15.6", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), 15.555f);
        assertEquals("15.6", sb.toString());


        SVGUtils.appendFloat(sb.delete(0, sb.length()), -1.0f);
        assertEquals("-1", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), -1.1f);
        assertEquals("-1.1", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), -1.11f);
        assertEquals("-1.1", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), -1.111f);
        assertEquals("-1.1", sb.toString());


        SVGUtils.appendFloat(sb.delete(0, sb.length()), -5.0f);
        assertEquals("-5", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), -5.5f);
        assertEquals("-5.5", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), -5.55f);
        assertEquals("-5.6", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), -5.555f);
        assertEquals("-5.6", sb.toString());


        SVGUtils.appendFloat(sb.delete(0, sb.length()), -15.0f);
        assertEquals("-15", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), -15.5f);
        assertEquals("-15.5", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), -15.55f);
        assertEquals("-15.6", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), -15.555f);
        assertEquals("-15.6", sb.toString());


        SVGUtils.appendFloat(sb.delete(0, sb.length()), 9.0f);
        assertEquals("9", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), 9.9f);
        assertEquals("9.9", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), 9.99f);
        assertEquals("10", sb.toString());

        SVGUtils.appendFloat(sb.delete(0, sb.length()), 9.999f);
        assertEquals("10", sb.toString());

    }

}