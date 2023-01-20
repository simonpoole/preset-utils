<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:m="http://josm.openstreetmap.de/tagging-preset-1.0">
    <!-- match reference elements in combos and multiselects and replace them -->
    <xsl:template match="m:multiselect/m:reference[@ref]|m:combo/m:reference[@ref]">
        <xsl:variable name="tmp" select="@ref" />
        <xsl:copy-of select="/m:presets/m:chunk[@id=$tmp]/*"/>
    </xsl:template>
    <!-- remove the chunks now -->
    <xsl:template match="/*/m:chunk[m:list_entry]" />
    <!--identity template copies everything forward by default -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>