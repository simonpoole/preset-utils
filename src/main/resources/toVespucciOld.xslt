<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:m="http://josm.openstreetmap.de/tagging-preset-1.0">
    <!--empty template suppresses attributes -->
    <xsl:template match="@name[../@deprecated]">
        <xsl:attribute name="name">
            <xsl:value-of select="." />
            <xsl:text> (deprecated)</xsl:text>
        </xsl:attribute>
    </xsl:template>
    <xsl:template match="@short_description[../@deprecated]">
        <xsl:attribute name="short_description">
            <xsl:value-of select="." />
            <xsl:text> (deprecated)</xsl:text>
        </xsl:attribute>
    </xsl:template>
    <!--identity template copies everything forward by default -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>