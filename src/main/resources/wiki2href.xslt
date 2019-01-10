<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:m="http://josm.openstreetmap.de/tagging-preset-1.0">
    <!--transform wiki to href= -->
    <xsl:template match="m:link/@wiki">
        <xsl:attribute name="href">
            <xsl:value-of
            select="concat('https://wiki.openstreetmap.org/wiki/', .)" />
        </xsl:attribute>
    </xsl:template>
    <!--identity template copies everything forward by default -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>