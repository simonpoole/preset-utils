# Preset utils

A couple of utils for processing JOSM format presets for OSM editors.

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