<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:m="http://josm.openstreetmap.de/tagging-preset-1.0" >
    <!--empty template suppresses attributes-->
    <xsl:template match="@deprecated" />
    <xsl:template match="@autoapply" />
    <xsl:template match="@regions" />
    <xsl:template match="@exclude_regions" />
    <xsl:template match="@long_text" />
    <xsl:template match="@i18n" />
    <xsl:template match="@value_type" />
    <xsl:template match="@value_count_key" />
    <xsl:template match="m:key/@values_context" />
    <xsl:template match="m:key/@text" />
    <xsl:template match="m:presets/@object_keys" />
    <xsl:template match="m:checkgroup/@text" />
    <xsl:template match="m:checkgroup/@text_context" />
    <xsl:template match="m:group/@items_sort" />
    <xsl:template match="m:item/@min_match" />
    <xsl:template match="m:preset_link/@alternative" />
    <!-- this removes the whole item -->
    <xsl:template match="*[m:text[@javascript]]" />
    <!--identity template copies everything forward by default-->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
