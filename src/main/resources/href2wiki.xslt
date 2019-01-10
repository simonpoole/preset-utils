<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:m="http://josm.openstreetmap.de/tagging-preset-1.0"
    xmlns:str="http://exslt.org/strings"
    xmlns:exsl="http://exslt.org/common">
    <!--empty template suppresses attributes -->
    <xsl:template
        match="m:link/@*[contains(name(), '.href')]" />
    <!--transform href to wiki= -->
    <xsl:template match="m:link/@href">
        <xsl:attribute name="wiki">
            <xsl:variable
            name="tmp"
            select="str:replace(., 'https://wiki.openstreetmap.org/wiki/', '')" />
            <xsl:value-of
            select="str:replace($tmp, 'http://wiki.openstreetmap.org/wiki/', '')" />
        </xsl:attribute>
    </xsl:template>
    <!--identity template copies everything forward by default -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>