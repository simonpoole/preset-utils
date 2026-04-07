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
    <xsl:template match="@name_template">
        <xsl:attribute name="name_template">
            <!-- this isn't really correct -->
            <xsl:call-template name="replace-string">
                <xsl:with-param name="text" select="." />
                <xsl:with-param name="search">{%</xsl:with-param>
                <xsl:with-param name="replace">{</xsl:with-param>
            </xsl:call-template>
        </xsl:attribute>
    </xsl:template>
    <!--identity template copies everything forward by default -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>
    
    <!-- Named template for string replacement -->
    <xsl:template name="replace-string">
        <xsl:param name="text"/>
        <xsl:param name="search"/>
        <xsl:param name="replace"/>
        <xsl:choose>
            <xsl:when test="contains($text, $search)">
                <xsl:value-of select="substring-before($text, $search)"/>
                <xsl:value-of select="$replace"/>
                <xsl:call-template name="replace-string">
                    <xsl:with-param name="text" select="substring-after($text, $search)"/>
                    <xsl:with-param name="search" select="$search"/>
                    <xsl:with-param name="replace" select="$replace"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$text"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>