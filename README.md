# MMAX2 to IOB/IeXML Converter
Converts MMAX2 files into IOB or IEXML format.

## Build
This project uses Maven to build. Type `mvn clean package` to create the executable JAR. This JAR will be found
in the `target/` directory after the build and have the prefix `jar-with-dependencies.jar`.

## Usage
After building the program can be run by typing `java -jar <path to jar-with-dependencies file>`. Then follow
the given options.

# Priolist

The program requires a text file representing a *priority list*. This refers to the annotation levels present in 
MMAX2 data that should be converted and is used to resolve cases in which multiple annotations overlap. The
output formats do not support overlapping annotations. The annotation name that comes first in the priority file
will thus hide annotations of subsequent annotation names in case of overlapping when converting to IOB or IeXML.

The default priolist for the FSU PRGE corpus, for example, is

    protein
    protein_familiy_or_group
    protein_enum
    protein_variant
    protein_complex
