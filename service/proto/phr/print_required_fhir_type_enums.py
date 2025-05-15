"""This file is not used in production and is intended to make the maintenance of the proto
definition easier. This will print the required fhir type enums which can be copy and pasted to
the fhirspec.proto file.

Example usage: /main/packages/modules/HealthFitness$
python3 -B service/proto/phr/print_required_fhir_type_enums.py \
../../../external/fhir/spec/r4/json_definitions/profiles-resources.json \
../../../external/fhir/spec/r4/json_definitions/profiles-types.json
"""

import sys
import json
from fhir_spec_utils import *


def extract_and_print_type_enums(profiles_resources_json, profiles_types_json, resources_set):
    top_level_fhir_types = set()
    # Child types are complex types that are not independent types, but are defined by a resource
    # type or complex type. They are of type BackboneElement if defined in a resource, and Element
    # if defined in a complex type.
    top_level_child_types = set()
    for resource, structure_definition in extract_type_to_structure_definitions_from_spec(
            profiles_resources_json, resources_set).items():
        fhir_types, child_types = _get_types_and_child_types_from_structure_definition(
            resource, structure_definition)
        top_level_fhir_types.update(fhir_types)
        top_level_child_types.update(child_types)

    all_required_fhir_types = _get_all_types_and_subtypes_from_type_definitions(
        profiles_types_json, top_level_fhir_types).union(top_level_child_types)
    # Add id type manually as the Resource ids use System.String in the spec
    all_required_fhir_types.add("id")
    # Add Element type manually as this is the type of the primitive type extensions
    all_required_fhir_types.add("Element")

    for i, data_type in enumerate(sorted(all_required_fhir_types, key=str.casefold)):
        print("R4_FHIR_TYPE_" + to_upper_snake_case(data_type) + " = " + str(i + 1) + ";")


def _get_types_and_child_types_from_structure_definition(type_name, structure_definition):
    fhir_type_strings = set()
    fhir_child_type_strings = set()

    # We don't need to extract subtypes for primitive types
    if structure_definition["kind"] == "primitive-type":
        return fhir_type_strings, fhir_child_type_strings

    for element_definition in extract_element_definitions_from_structure_def(
            structure_definition):
        if element_definition["id"] == type_name or "contentReference" in element_definition:
            continue

        for data_type in element_definition["type"]:
            type_code = data_type["code"]
            if type_code in ["BackboneElement", "Element"]:
                # This means the type is a child type defined by the complex type or resource
                child_type_string = generate_fhir_type_string_for_child_type_path(
                    element_definition["path"])
                fhir_child_type_strings.add(child_type_string)
            elif type_code != "http://hl7.org/fhirpath/System.String":
                fhir_type_strings.add(type_code)

    return fhir_type_strings, fhir_child_type_strings


def _get_all_types_and_subtypes_from_type_definitions(profiles_types_json, types_set):
    type_to_structure_definition = extract_type_to_structure_definitions_from_spec(
        profiles_types_json, None)

    extracted_sub_types, extracted_child_types = (
        _recursively_get_all_types_and_child_types_from_type_definitions(
            type_to_structure_definition, types_set))

    return extracted_sub_types.union(extracted_child_types)


def _recursively_get_all_types_and_child_types_from_type_definitions(
        type_to_structure_definition, types_to_extract_set, extracted_types_set=set(),
        extracted_child_types=set()):
    new_types_to_extract_set = set()
    new_extracted_child_types = set()

    for fhir_type in types_to_extract_set:
        if fhir_type in ["Resource", "Element", "BackboneElement"]:
            # "Resource" type is not defined in the profile types file, and we don't need it as
            # contained resources are disallowed at the moment.
            # Element and BackboneElement are child types that are not defined in the profile types
            # file, because they are defined in the top level types directly
            continue

        sub_types, child_types = _get_types_and_child_types_from_structure_definition(
            fhir_type, type_to_structure_definition[fhir_type])
        new_extracted_child_types.update(child_types)
        for sub_type in sub_types:
            if sub_type not in extracted_types_set and sub_type not in types_to_extract_set:
                new_types_to_extract_set.add(sub_type)

    if new_types_to_extract_set:
        return _recursively_get_all_types_and_child_types_from_type_definitions(
            type_to_structure_definition, new_types_to_extract_set,
            extracted_types_set.union(types_to_extract_set),
            extracted_child_types.union(new_extracted_child_types))

    else:
        return extracted_types_set.union(types_to_extract_set), extracted_child_types.union(
            new_extracted_child_types)


if __name__ == '__main__':
    resource_definitions_file_name = sys.argv[1]
    type_definitions_file_name = sys.argv[2]

    with open(resource_definitions_file_name, 'r') as resources_file, open(
            type_definitions_file_name, 'r') as types_file:
        resource_definitions = json.load(resources_file)
        type_definitions = json.load(types_file)

        extract_and_print_type_enums(resource_definitions, type_definitions,
                                     HC_SUPPORTED_RESOURCE_SET)
