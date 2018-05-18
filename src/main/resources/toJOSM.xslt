<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:m="http://josm.openstreetmap.de/tagging-preset-1.0" >
    <!--empty template suppresses attributes-->
    <xsl:template match="@deprecated" />
    <xsl:template match="@region" />
    <xsl:template match="@long_text" />
    <xsl:template match="@i18n" />
    <xsl:template match="@javascript" />
    <xsl:template match="@value_type" />
    <xsl:template match="m:key/@values_context" />
    <xsl:template match="m:key/@text" />
    <xsl:template match="m:presets/@object_keys" />
    <xsl:template match="m:checkgroup/@name" />
    <xsl:template match="m:checkgroup/@radiogroup" />
    <!--identity template copies everything forward by default-->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
