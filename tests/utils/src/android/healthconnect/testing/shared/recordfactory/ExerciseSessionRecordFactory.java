/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.healthconnect.testing.shared.recordfactory;

import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT;

import android.health.connect.datatypes.ExerciseLap;
import android.health.connect.datatypes.ExerciseRoute;
import android.health.connect.datatypes.ExerciseSegment;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.units.Length;
import android.os.Bundle;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class ExerciseSessionRecordFactory extends RecordFactory<ExerciseSessionRecord> {
    private static final String KEY_EXERCISE_TYPE = PREFIX + "EXERCISE_TYPE";
    private static final String KEY_TITLE = PREFIX + "TITLE";
    private static final String KEY_NOTES = PREFIX + "NOTES";
    private static final String KEY_LAPS = PREFIX + "LAPS";
    private static final String KEY_SEGMENTS = PREFIX + "SEGMENTS";
    private static final String KEY_ROUTE = PREFIX + "ROUTE";
    private static final String KEY_HAS_ROUTE = PREFIX + "HAS_ROUTE";
    private static final String KEY_PLANNED_EXERCISE_SESSION_ID =
            PREFIX + "PLANNED_EXERCISE_SESSION_ID";

    @Override
    public ExerciseSessionRecord newFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new ExerciseSessionRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_WALKING)
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .setTitle("My walking session")
                .setNotes("A long walk in the park")
                .setLaps(
                        List.of(
                                new ExerciseLap.Builder(startTime, endTime)
                                        .setLength(Length.fromMeters(10))
                                        .build()))
                .setSegments(
                        List.of(
                                new ExerciseSegment.Builder(
                                                startTime,
                                                endTime,
                                                EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT)
                                        .setRepetitionsCount(10)
                                        .build()))
                .setRoute(
                        new ExerciseRoute(
                                List.of(
                                        new ExerciseRoute.Location.Builder(
                                                        startTime.plusMillis(1), 12.3, 45.6)
                                                .setAltitude(Length.fromMeters(100.0))
                                                .setHorizontalAccuracy(Length.fromMeters(10.0))
                                                .setVerticalAccuracy(Length.fromMeters(20.0))
                                                .build(),
                                        new ExerciseRoute.Location.Builder(
                                                        startTime.plusMillis(2), 13.4, 46.7)
                                                .setAltitude(Length.fromMeters(100.0))
                                                .setHorizontalAccuracy(Length.fromMeters(11.0))
                                                .setVerticalAccuracy(Length.fromMeters(22.0))
                                                .build())))
                .build();
    }

    @Override
    public ExerciseSessionRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new ExerciseSessionRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING)
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .setTitle("My running session")
                .setNotes("A quick run around the block")
                .setLaps(
                        List.of(
                                new ExerciseLap.Builder(startTime, endTime)
                                        .setLength(Length.fromMeters(20))
                                        .build()))
                .setSegments(
                        List.of(
                                new ExerciseSegment.Builder(
                                                startTime,
                                                endTime,
                                                EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT)
                                        .setRepetitionsCount(20)
                                        .build()))
                .setRoute(
                        new ExerciseRoute(
                                List.of(
                                        new ExerciseRoute.Location.Builder(
                                                        startTime.plusMillis(3), 22.3, 55.6)
                                                .setHorizontalAccuracy(Length.fromMeters(10.0))
                                                .setVerticalAccuracy(Length.fromMeters(20.0))
                                                .build(),
                                        new ExerciseRoute.Location.Builder(
                                                        startTime.plusMillis(4), 23.3, 56.6)
                                                .setHorizontalAccuracy(Length.fromMeters(11.0))
                                                .setVerticalAccuracy(Length.fromMeters(22.0))
                                                .build())))
                .build();
    }

    @Override
    public ExerciseSessionRecord newEmptyRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new ExerciseSessionRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_UNKNOWN)
                .build();
    }

    @Override
    protected ExerciseSessionRecord recordWithMetadata(
            ExerciseSessionRecord record, Metadata metadata) {
        return new ExerciseSessionRecord.Builder(
                        metadata,
                        record.getStartTime(),
                        record.getEndTime(),
                        record.getExerciseType())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .setTitle(record.getTitle())
                .setNotes(record.getNotes())
                .setLaps(record.getLaps())
                .setSegments(record.getSegments())
                .setRoute(record.getRoute())
                .build();
    }

    private static final String KEY_LAPS_START_TIMES = KEY_LAPS + "_START_TIMES";
    private static final String KEY_LAPS_END_TIMES = KEY_LAPS + "_END_TIMES";
    private static final String KEY_LAPS_LENGTHS = KEY_LAPS + "_LENGTHS";
    private static final String KEY_SEGMENTS_START_TIMES = KEY_SEGMENTS + "_START_TIMES";
    private static final String KEY_SEGMENTS_END_TIMES = KEY_SEGMENTS + "_END_TIMES";
    private static final String KEY_SEGMENTS_TYPES = KEY_SEGMENTS + "_TYPES";
    private static final String KEY_SEGMENTS_REPS = KEY_SEGMENTS + "_REPS";
    private static final String KEY_ROUTE_LOCATIONS = KEY_ROUTE + "_LOCATIONS";
    private static final String KEY_LOCATION_TIME = "time";
    private static final String KEY_LOCATION_LATITUDE = "latitude";
    private static final String KEY_LOCATION_LONGITUDE = "longitude";
    private static final String KEY_LOCATION_ALTITUDE = "altitude";
    private static final String KEY_LOCATION_HORIZONTAL_ACCURACY = "horizontalAccuracy";
    private static final String KEY_LOCATION_VERTICAL_ACCURACY = "verticalAccuracy";

    @Override
    protected Bundle getValuesBundleForRecord(ExerciseSessionRecord record) {
        Bundle values = new Bundle();
        values.putInt(KEY_EXERCISE_TYPE, record.getExerciseType());
        values.putCharSequence(KEY_TITLE, record.getTitle());
        values.putCharSequence(KEY_NOTES, record.getNotes());

        long[] lapStartTimes =
                record.getLaps().stream()
                        .mapToLong(lap -> lap.getStartTime().toEpochMilli())
                        .toArray();
        long[] lapEndTimes =
                record.getLaps().stream()
                        .mapToLong(lap -> lap.getEndTime().toEpochMilli())
                        .toArray();
        double[] lapLengths =
                record.getLaps().stream()
                        .mapToDouble(
                                lap -> lap.getLength() == null ? -1 : lap.getLength().getInMeters())
                        .toArray();
        values.putLongArray(KEY_LAPS_START_TIMES, lapStartTimes);
        values.putLongArray(KEY_LAPS_END_TIMES, lapEndTimes);
        values.putDoubleArray(KEY_LAPS_LENGTHS, lapLengths);

        long[] segmentStartTimes =
                record.getSegments().stream()
                        .mapToLong(segment -> segment.getStartTime().toEpochMilli())
                        .toArray();
        long[] segmentEndTimes =
                record.getSegments().stream()
                        .mapToLong(segment -> segment.getEndTime().toEpochMilli())
                        .toArray();
        ArrayList<Integer> segmentTypes = new ArrayList<>();
        ArrayList<Integer> segmentRepetitions = new ArrayList<>();
        for (ExerciseSegment segment : record.getSegments()) {
            segmentTypes.add(segment.getSegmentType());
            segmentRepetitions.add(segment.getRepetitionsCount());
        }
        values.putLongArray(KEY_SEGMENTS_START_TIMES, segmentStartTimes);
        values.putLongArray(KEY_SEGMENTS_END_TIMES, segmentEndTimes);
        values.putIntegerArrayList(KEY_SEGMENTS_TYPES, segmentTypes);
        values.putIntegerArrayList(KEY_SEGMENTS_REPS, segmentRepetitions);

        values.putBoolean(KEY_HAS_ROUTE, record.hasRoute());
        if (record.getRoute() != null) {
            Bundle routeBundle = new Bundle();
            ArrayList<Bundle> locationBundles = new ArrayList<>();
            for (ExerciseRoute.Location location : record.getRoute().getRouteLocations()) {
                Bundle locationBundle = new Bundle();
                locationBundle.putLong(KEY_LOCATION_TIME, location.getTime().toEpochMilli());
                locationBundle.putDouble(KEY_LOCATION_LATITUDE, location.getLatitude());
                locationBundle.putDouble(KEY_LOCATION_LONGITUDE, location.getLongitude());
                if (location.getAltitude() != null) {
                    locationBundle.putDouble(
                            KEY_LOCATION_ALTITUDE, location.getAltitude().getInMeters());
                }
                if (location.getHorizontalAccuracy() != null) {
                    locationBundle.putDouble(
                            KEY_LOCATION_HORIZONTAL_ACCURACY,
                            location.getHorizontalAccuracy().getInMeters());
                }
                if (location.getVerticalAccuracy() != null) {
                    locationBundle.putDouble(
                            KEY_LOCATION_VERTICAL_ACCURACY,
                            location.getVerticalAccuracy().getInMeters());
                }
                locationBundles.add(locationBundle);
            }
            routeBundle.putParcelableArrayList(KEY_ROUTE_LOCATIONS, locationBundles);
            values.putBundle(KEY_ROUTE, routeBundle);
        }

        if (record.getPlannedExerciseSessionId() != null) {
            values.putString(KEY_PLANNED_EXERCISE_SESSION_ID, record.getPlannedExerciseSessionId());
        }

        return values;
    }

    @Override
    public ExerciseSessionRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        ExerciseSessionRecord.Builder builder =
                new ExerciseSessionRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                bundle.getInt(
                                        KEY_EXERCISE_TYPE,
                                        ExerciseSessionType.EXERCISE_SESSION_TYPE_UNKNOWN))
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset);
        if (bundle.containsKey(KEY_TITLE)) {
            builder.setTitle(bundle.getString(KEY_TITLE));
        }
        if (bundle.containsKey(KEY_NOTES)) {
            builder.setNotes(bundle.getString(KEY_NOTES));
        }

        long[] lapStartTimes = bundle.getLongArray(KEY_LAPS_START_TIMES);
        if (lapStartTimes != null) {
            long[] lapEndTimes = bundle.getLongArray(KEY_LAPS_END_TIMES);
            double[] lapLengths = bundle.getDoubleArray(KEY_LAPS_LENGTHS);
            List<ExerciseLap> laps = new ArrayList<>();
            for (int i = 0; i < lapStartTimes.length; i++) {
                ExerciseLap.Builder lapBuilder =
                        new ExerciseLap.Builder(
                                Instant.ofEpochMilli(lapStartTimes[i]),
                                Instant.ofEpochMilli(lapEndTimes[i]));
                if (lapLengths[i] != -1) {
                    lapBuilder.setLength(Length.fromMeters(lapLengths[i]));
                }
                laps.add(lapBuilder.build());
            }
            builder.setLaps(laps);
        }

        long[] segmentStartTimes = bundle.getLongArray(KEY_SEGMENTS_START_TIMES);
        if (segmentStartTimes != null) {
            long[] segmentEndTimes = bundle.getLongArray(KEY_SEGMENTS_END_TIMES);
            ArrayList<Integer> segmentTypes = bundle.getIntegerArrayList(KEY_SEGMENTS_TYPES);
            ArrayList<Integer> segmentRepetitions = bundle.getIntegerArrayList(KEY_SEGMENTS_REPS);
            List<ExerciseSegment> segments = new ArrayList<>();
            for (int i = 0; i < segmentStartTimes.length; i++) {
                segments.add(
                        new ExerciseSegment.Builder(
                                        Instant.ofEpochMilli(segmentStartTimes[i]),
                                        Instant.ofEpochMilli(segmentEndTimes[i]),
                                        segmentTypes.get(i))
                                .setRepetitionsCount(segmentRepetitions.get(i))
                                .build());
            }
            builder.setSegments(segments);
        }

        if (!bundle.containsKey(KEY_ROUTE) && bundle.getBoolean(KEY_HAS_ROUTE)) {
            // Handle the `route == null && hasRoute == true` case which is a valid state.
            setHasRoute(builder, true);
        }
        if (bundle.containsKey(KEY_ROUTE)) {
            Bundle routeBundle = bundle.getBundle(KEY_ROUTE);
            ArrayList<Bundle> locationBundles =
                    routeBundle.getParcelableArrayList(KEY_ROUTE_LOCATIONS);
            List<ExerciseRoute.Location> locations = new ArrayList<>();
            for (Bundle locationBundle : locationBundles) {
                ExerciseRoute.Location.Builder locationBuilder =
                        new ExerciseRoute.Location.Builder(
                                Instant.ofEpochMilli(locationBundle.getLong(KEY_LOCATION_TIME)),
                                locationBundle.getDouble(KEY_LOCATION_LATITUDE),
                                locationBundle.getDouble(KEY_LOCATION_LONGITUDE));
                if (locationBundle.containsKey(KEY_LOCATION_ALTITUDE)) {
                    locationBuilder.setAltitude(
                            Length.fromMeters(locationBundle.getDouble(KEY_LOCATION_ALTITUDE)));
                }
                if (locationBundle.containsKey(KEY_LOCATION_HORIZONTAL_ACCURACY)) {
                    locationBuilder.setHorizontalAccuracy(
                            Length.fromMeters(
                                    locationBundle.getDouble(KEY_LOCATION_HORIZONTAL_ACCURACY)));
                }
                if (locationBundle.containsKey(KEY_LOCATION_VERTICAL_ACCURACY)) {
                    locationBuilder.setVerticalAccuracy(
                            Length.fromMeters(
                                    locationBundle.getDouble(KEY_LOCATION_VERTICAL_ACCURACY)));
                }
                locations.add(locationBuilder.build());
            }
            builder.setRoute(new ExerciseRoute(locations));
        }

        if (bundle.containsKey(KEY_PLANNED_EXERCISE_SESSION_ID)) {
            builder.setPlannedExerciseSessionId(bundle.getString(KEY_PLANNED_EXERCISE_SESSION_ID));
        }

        return builder.build();
    }

    /**
     * Calls {@code ExerciseSessionRecord.Builder.setHasRoute} using reflection as the method is
     * hidden.
     */
    private static void setHasRoute(ExerciseSessionRecord.Builder record, boolean hasRoute) {
        // Getting a hidden method by its signature using getMethod() throws an exception in test
        // apps, but iterating throw all the methods and getting the needed one works.
        for (var method : record.getClass().getMethods()) {
            if (method.getName().equals("setHasRoute")) {
                try {
                    method.invoke(record, hasRoute);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
    }

    @Override
    public String recordToString(ExerciseSessionRecord record) {
        return "ExerciseSessionRecord {\n"
                + "\tstartTime = "
                + record.getStartTime()
                + ",\n\tendTime = "
                + record.getEndTime()
                + ",\n\tstartZoneOffset = "
                + record.getStartZoneOffset()
                + ",\n\tendZoneOffset = "
                + record.getEndZoneOffset()
                + ",\n\tmetadata = "
                + metadataToString(record.getMetadata())
                + ",\n\texerciseType = "
                + record.getExerciseType()
                + ",\n\ttitle = "
                + record.getTitle()
                + ",\n\tnotes = "
                + record.getNotes()
                + ",\n\tlaps = "
                + lapsToString(record.getLaps())
                + ",\n\tsegments = "
                + segmentsToString(record.getSegments())
                + ",\n\thasRoute = "
                + record.hasRoute()
                + ",\n\troute = "
                + routeToString(record.getRoute())
                + ",\n\tplannedExerciseSessionId = "
                + record.getPlannedExerciseSessionId()
                + "\n}";
    }

    private static String lapsToString(List<ExerciseLap> laps) {
        if (laps == null) {
            return "null";
        }

        return "Laps{"
                + "\n\t\tsize="
                + laps.size()
                + ",\n\t\tlist="
                + laps.stream().map(ExerciseSessionRecordFactory::lapToString).toList()
                + "\n\t}";
    }

    private static String lapToString(ExerciseLap lap) {
        return "Lap{"
                + "\n\t\t\tstartTime="
                + lap.getStartTime()
                + ",\n\t\t\tendTime="
                + lap.getEndTime()
                + ",\n\t\t\tlength="
                + lap.getLength()
                + "\n\t\t}";
    }

    private static String segmentsToString(List<ExerciseSegment> segments) {
        if (segments == null) {
            return "null";
        }

        return "Segments{"
                + "\n\t\tsize="
                + segments.size()
                + ",\n\t\tlist="
                + segments.stream().map(ExerciseSessionRecordFactory::segmentToString).toList()
                + "\n\t}";
    }

    private static String segmentToString(ExerciseSegment segment) {
        return "Segment{"
                + "\n\t\t\tstartTime="
                + segment.getStartTime()
                + ",\n\t\t\tendTime="
                + segment.getEndTime()
                + ",\n\t\t\tsegmentType="
                + segment.getSegmentType()
                + ",\n\t\t\trepetitionsCount="
                + segment.getRepetitionsCount()
                + "\n\t\t}";
    }

    private static String routeToString(ExerciseRoute route) {
        if (route == null) {
            return "null";
        }

        return "Route{"
                + "\n\t\trouteLocations="
                + route.getRouteLocations().stream()
                        .map(ExerciseSessionRecordFactory::locationToString)
                        .toList()
                + "\n\t}";
    }

    private static String locationToString(ExerciseRoute.Location location) {
        return "Location{"
                + "\n\t\t\ttime="
                + location.getTime()
                + ",\n\t\t\tlatitude="
                + location.getLatitude()
                + ",\n\t\t\tlongitude="
                + location.getLongitude()
                + ",\n\t\t\taltitude="
                + location.getAltitude()
                + ",\n\t\t\thorizontalAccuracy="
                + location.getHorizontalAccuracy()
                + ",\n\t\t\tverticalAccuracy="
                + location.getVerticalAccuracy()
                + "\n\t\t}";
    }
}
