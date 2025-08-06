<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:m="http://josm.openstreetmap.de/tagging-preset-1.0"
    xmlns:str="http://exslt.org/strings"
    extension-element-prefixes="str">
    <!--empty template suppresses attributes -->
    <xsl:template match="@name[../@deprecated]">
        <xsl:attribute name="name">
            <xsl:value-of select="." />
            <xsl:text> (deprecated)</xsl:text>
        </xsl:attribute>
    </xsl:template>
    <xsl:template match="@text[../@deprecated]">
        <xsl:attribute name="text">
            <xsl:value-of select="." />
            <xsl:text> (deprecated)</xsl:text>
        </xsl:attribute>
    </xsl:template>
    <xsl:template match="@display_value[../@deprecated]">
        <xsl:attribute name="display_value">
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
    <xsl:template match="@type[contains(.,'area')]">
        <xsl:attribute name="type">
            <xsl:for-each select="str:tokenize(.,',')">
              <xsl:choose>
                <xsl:when test=".='area'">
                  <xsl:text>closedway,multipolygon</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                  <!-- don't output closedway and multipolygon values that are already present -->
                  <xsl:if test=". != 'closedway' and .!= 'multipolygon'">
                    <xsl:value-of select="." />
                  </xsl:if>
                </xsl:otherwise>
              </xsl:choose>
              <xsl:if test="position() != last()">,</xsl:if>  
            </xsl:for-each>
        </xsl:attribute>
    </xsl:template>
    <xsl:template match="@deprecated" />
    <xsl:template match="@autoapply" />
    <xsl:template match="@long_text" />
    <xsl:template match="@i18n" />
    <xsl:template match="@value_type" />
    <xsl:template match="@value_count_key" />
    <xsl:template match="@image" />
    <xsl:template match="m:key/@values_context" />
    <xsl:template match="m:key/@text" />
    <xsl:template match="m:key/@object" />
    <xsl:template match="m:key/@regions" />
    <xsl:template match="m:key/@exclude_regions" />
    <xsl:template match="m:presets/@object_keys" />
    <xsl:template match="m:checkgroup/@text" />
    <xsl:template match="m:checkgroup/@text_context" />
    <xsl:template match="m:group/@items_sort" />
    <xsl:template match="m:item/@min_match" />
    <xsl:template match="m:preset_link/@alternative" />
    <!-- this removes the whole item -->
    <xsl:template match="*[m:text[@javascript]]" />
    <!--identity template copies everything forward by default -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>