/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.healthconnect.testing.shared.phr;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Abstract super class for helper classes for building FHIR resources for testing.
 *
 * <p>Subclasses should implement {@link #returnThis()} as:
 *
 * <pre>{@code
 * @Override
 * protected XxxBuilder returnThis() {
 *   return this;
 * }
 * }</pre>
 *
 * This enables the type information to work correctly.
 *
 * @param <T> the class of the subclass which is the concrete class.
 */
abstract class FhirResourceBuilder<T extends FhirResourceBuilder<T>> {
    private final JSONObject mFhir;

    FhirResourceBuilder(String json) {
        try {
            this.mFhir = new JSONObject(json);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Help the subclasses return the correct type.
     *
     * @return the current object, correctly typed.
     */
    protected abstract T returnThis();

    /**
     * Set the FHIR id for the resource.
     *
     * @return this Builder.
     */
    public T setId(String id) {
        return set("id", id);
    }

    /**
     * Sets an arbitrary String or JSON Object element in the FHIR resource.
     *
     * @param field the element to set.
     * @param value the value to set
     * @return this builder
     */
    public T set(String field, Object value) {
        try {
            mFhir.put(field, value);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
        return returnThis();
    }

    /**
     * Removes json element from the FHIR resource.
     *
     * @param field the element to remove.
     * @return this builder
     */
    public T removeField(String field) {
        mFhir.remove(field);
        return returnThis();
    }

    /** Returns the current state of this builder as a JSON FHIR string. */
    public String toJson() {
        try {
            return mFhir.toString(/* indentSpaces= */ 2);
        } catch (JSONException e) {
            // Should never happen, but JSONException is declared, and is a checked exception.
            throw new IllegalStateException(e);
        }
    }
}
