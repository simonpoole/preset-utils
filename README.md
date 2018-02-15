# Preset utils

A couple of utils for processing JOSM format presets for OSM editors and related stuff.

## Preset2Pot

Generates a [gettext](https://www.gnu.org/software/gettext/) format translation template file from the preset file.

### Usage

    -i,--input <arg>    input preset file, default: standard in
    -o,--output <arg>   output .pot file, default: standard out

## Preset2Html

Generate a simple HTML page display the contents of the preset file.

Replaces 

__${ICONPATH}__ with _icons/png/_

__${ICONTYPE}__ with _.png_

with other words it assumes that the icons are available in PNG format in _icons/png_.

Further it expects _preset.css_ in the directory _website_

### Usage

    -i,--input <arg>      input preset file, default: standard in
    -j,--josm <arg>       download link JOSM format, default: none
    -o,--output <arg>     output .html file, default: standard out
    -v,--vespucci <arg>   download link vespucci format, default: none
    
## Synonyms

Retrieve synonym values from the iD editor repository and output a json object containing them.

### Usage
 
     -l,--lang <arg>       language code to retrieve the synonyms for, default: en
     -o,--output <arg>     output json file, default: standard out
     -x,--exclude <args>   skip any entries that contain the terms (for example "military")
     
## ID2JOSM

Generate a JOSM preset from the iD preset configuration, retrieving some values from taginfo similar to what iD does.

Note this does not include any icons from the preset and is mainly useful for statistical and comparison purposes. Further it uses some extensions to the original JOSM schema that need to be removed if you actually want to use the output in JOSM (use _xmlstarlet tr src/main/resources/toJOSM.xslt ..._ to do that)

### Usage

     -o,--output <arg>     output xml file, default: standard out
     -c,--chunk            if set iD field definitions will be converted to JOSM preset "chunks", if not set the fields are expanded inline