import argparse
import glob
import zipfile
import os
import shutil
import sys


def remove_duplicate_properties(prop_files):
    unique = {}
    for path in prop_files:
        filename = os.path.basename(path)
        if filename not in unique:
            unique[filename] = path
    return list(unique.values())


def get_settings_from_properties(
    out_f, tmp_zip_dir: str, include_comments: bool, verbose: bool
) -> None:
    # find all *preferences.properties files
    pattern = tmp_zip_dir + "/**/*preferences.properties"
    prop_files = glob.glob(pattern, recursive=True)
    prop_files_unique = remove_duplicate_properties(prop_files)

    package_str = ""
    for prop_file in prop_files_unique:
        if verbose:
            print("+ {}".format(prop_file).replace("/./tmp-zip/", "/"))
        with open(prop_file, "r") as file:
            lines = file.readlines()
        for line in lines:
            line = line.strip()
            if line.startswith("# Package "):
                package_str = line[10:].strip()
                # print package name with number signs above and below
                if include_comments:
                    out_f.write("\n{0}\n{1}\n{0}\n".format("#" * (len(line)), line))
                else:
                    out_f.write("\n{0}\n{1}\n{0}\n\n".format("#" * (len(line)), line))
                # if verbose:
                # print("| {} ({})".format(" " * len(jar_file), package_str))
            elif "--------" in line:
                continue
            # assume equal sign means this is a property
            elif "=" in line:
                if line[0] == "#":
                    if include_comments:
                        out_f.write("# {}/{}\n".format(package_str, line[1:].strip()))
                else:
                    out_f.write("# {}/{}\n".format(package_str, line))
            # a few pva properties don't have equal sign so this covers those
            elif line != "" and line[0] != "#":
                out_f.write("# {}/{}\n".format(package_str, line))
            else:
                if include_comments:
                    out_f.write(line + "\n")


def create_settings_template(
    product_location: str,
    include_comments: bool,
    verbose: bool,
    properties_directory: str,
) -> None:
    """
    Create complete list of settings for settings.ini file

    :param product_location: Location of the product jar files to check for *preferences.properties files
    :param include_comments: Option to include comments for each setting in output file
    :param verbose: Verbose operation
    :param properties_directory: Using src directory instead of jar files
    """

    print("Creating settings_template.ini file...")
    output_file = "settings_template.ini"
    out_f = open(output_file, "w")
    out_f.write(
        "# Complete List of Available Preference Properties (Created by create_settings_template.py)\n"
    )

    if not properties_directory:
        print("Parsing properties in jar files")
        # find all jar files so we can unzip jar and find preference files
        jar_file_list = glob.glob(product_location + "/*.jar")
        if len(jar_file_list) <= 0:
            sys.stderr.write("No *.jar files found in '{}'\n".format(product_location))
            sys.stderr.write("Need to build sources?\n")
            sys.exit(-1)
        jar_file_list = sorted(jar_file_list, key=str.lower)

        # temp directory to hold unzipped jar file contents (deleted at end of script)
        tmp_zip_dir = "./tmp-zip"

        for jar_file in jar_file_list:
            if verbose:
                print("| {}".format(jar_file))
            if not os.path.isdir(tmp_zip_dir):
                os.makedirs(tmp_zip_dir)
            with zipfile.ZipFile(jar_file, "r") as zip_ref:
                zip_ref.extractall(tmp_zip_dir)

            get_settings_from_properties(out_f, tmp_zip_dir, include_comments, verbose)

            # remove temp directory
            shutil.rmtree(tmp_zip_dir)
    else:
        print(f"Parsing properties in : {args.directory}")
        get_settings_from_properties(
            out_f, properties_directory, include_comments, verbose
        )

    print("Creation complete")


parser = argparse.ArgumentParser(
    description="Create template of settings.ini with all available settings"
)
parser.add_argument(
    "product",
    type=str,
    nargs="?",
    default="./target/lib",
    help="Location of product jars. Defaults to ./target/lib",
)
parser.add_argument(
    "-c", "--comments", action="store_true", help="Include setting comments in file"
)
parser.add_argument("-v", "--verbose", action="store_true", help="Verbose operation")
parser.add_argument(
    "-d",
    "--directory",
    type=str,
    help="Specify the source directory instead of using compiled jar",
)
args = parser.parse_args()

print(args.directory)
create_settings_template(args.product, args.comments, args.verbose, args.directory)
