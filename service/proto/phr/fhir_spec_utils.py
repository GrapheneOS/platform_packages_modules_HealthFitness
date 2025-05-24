from typing import Mapping

# LINT.IfChange(fhir_resource_type_mapping)
RESOURCE_TYPE_STRING_TO_HC_INT_MAPPING = {
    "Immunization": 1,
    "AllergyIntolerance": 2,
    "Observation": 3,
    "Condition": 4,
    "Procedure": 5,
    "Medication": 6,
    "MedicationRequest": 7,
    "MedicationStatement": 8,
    "Patient": 9,
    "Practitioner": 10,
    "PractitionerRole": 11,
    "Encounter": 12,
    "Location": 13,
    "Organization": 14,
    "Device": 15,
}
# LINT.ThenChange(/framework/java/android/health/connect/datatypes/FhirResource.java)

HC_SUPPORTED_RESOURCE_SET = set(RESOURCE_TYPE_STRING_TO_HC_INT_MAPPING.keys())

FHIR_VERSION_R4 = "4.0.1"


def to_upper_snake_case(string: str) -> str:
    snake_case_string = string[0].upper() + "".join(
        ["_" + c if c.isupper() else c for c in string[1:]])

    return snake_case_string.upper().replace('-', '_')


def extract_type_to_structure_definitions_from_spec(
        definitions_json: Mapping, type_names: set[str]) -> Mapping:
    """Returns structure definitions by type for the requested type_names.

    The type name extraction is not case sensitive (everything gets converted to upper case) to make
    it easier to extract type definitions from enum names.

    Args:
            definitions_json: The contents of a fhir spec file with type definitions, which is in
            the structure of a https://hl7.org/fhir/Bundle.html, parsed to dict. The Bundle.entry
            will contain the list of https://hl7.org/fhir/StructureDefinition.html that we are
            interested in.
            type_names: The set of FHIR types to extract StructureDefinitions for. If type_names is
            None, all types will be extracted that have a url starting with
            "http://hl7.org/fhir/StructureDefinition/".

    Returns:
        The mapping of type name to StructureDefinition.
    """
    type_to_structure_definition = {}
    upper_case_type_names = None

    if type_names is not None:
        upper_case_type_names = set(name.upper() for name in type_names)
        if len(upper_case_type_names) != len(type_names):
            raise ValueError("Found type name duplicates after converting to upper case")

    for entry in definitions_json["entry"]:
        fullUrl = entry["fullUrl"]
        if not fullUrl.startswith("http://hl7.org/fhir/StructureDefinition/"):
            continue

        type_name = fullUrl.split("/")[-1]
        if type_names is not None and type_name.upper() not in upper_case_type_names:
            continue

        type_structure_definition = entry["resource"]

        # Do some assertions on expected values
        if type_structure_definition["fhirVersion"] != FHIR_VERSION_R4:
            raise ValueError("Unexpected fhir version found")
        # The definitions for SimpleQuantity and MoneyQuantity have type = Quantity
        if type_structure_definition["type"] != type_name and type_name not in [
            "SimpleQuantity", "MoneyQuantity"]:
            raise ValueError(
                f"Unexpected type in structure definition: {type_structure_definition['type']}" +
                "for {type_name}")

        if type_name in type_to_structure_definition:
            raise ValueError("Type definition already exists: " + type_name)
        type_to_structure_definition[type_name] = type_structure_definition

    if type_names is not None and len(type_to_structure_definition.keys()) != len(type_names):
        raise ValueError("Did not find type definitions for all requested types.")

    return type_to_structure_definition


def extract_element_definitions_from_structure_def(structure_definition):
    """Returns the list of "snapshot" ElementDefinitions from the provided StructureDefinition

    We select the list of elements in "snapshot" (as opposed to "differential"), as we want the full
    definition of fields, including fields from any base definitions. Each ElementDefinition
    contains the spec for a path / field of the type.
    """
    return structure_definition["snapshot"]["element"]

def generate_fhir_type_string_for_child_type_path(path):
    """Generates a type string from a path to be used to get the child type enum.

    The resulting string can be used in the same way as the type.code field in a
    StructureDefinition.

    For example, a path "Immunization.performer" will be turned into "ImmunizationPerformer".
    """
    return path.replace('.', ' ').title().replace(' ', '')
