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

package com.android.server.healthconnect.storage.request;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.storage.utils.OrderByClause;
import com.android.server.healthconnect.storage.utils.SqlJoin;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ReadTableRequestTest {

    @Test
    public void testGetReadQuery_hasTwoLimitsAndNoJoinClause_throws() {
        ReadTableRequest request = new ReadTableRequest("tableName").setLimit(5).setFinalLimit(5);

        assertThrows(IllegalArgumentException.class, () -> request.getReadCommand());
    }

    @Test
    public void testGetCountQuery_hasTwoLimitsAndNoJoinClause_throws() {
        ReadTableRequest request = new ReadTableRequest("tableName").setLimit(5).setFinalLimit(5);

        assertThrows(IllegalArgumentException.class, () -> request.getCountCommand());
    }

    @Test
    public void testGetReadQuery_hasTwoOrderByClauseAndNoJoinClause_throws() {
        OrderByClause orderByClause = new OrderByClause().addOrderByClause("columnName", true);
        ReadTableRequest request =
                new ReadTableRequest("tableName")
                        .setOrderBy(orderByClause)
                        .setFinalOrderBy(orderByClause);

        assertThrows(IllegalArgumentException.class, () -> request.getReadCommand());
    }

    @Test
    public void testGetCountQuery_hasTwoOrderByClauseAndNoJoinClause_throws() {
        OrderByClause orderByClause = new OrderByClause().addOrderByClause("columnName", true);
        ReadTableRequest request =
                new ReadTableRequest("tableName")
                        .setOrderBy(orderByClause)
                        .setFinalOrderBy(orderByClause);

        assertThrows(IllegalArgumentException.class, () -> request.getCountCommand());
    }

    @Test
    public void testGetReadCommand_simpleQuery() {
        ReadTableRequest request = new ReadTableRequest("tableName");

        assertThat(request.getReadCommand()).isEqualTo("SELECT * FROM tableName");
    }

    @Test
    public void testGetCountCommand_simpleQuery() {
        ReadTableRequest request = new ReadTableRequest("tableName");

        assertThat(request.getCountCommand()).isEqualTo("SELECT COUNT(*) FROM tableName");
    }

    @Test
    public void testGetReadCommand_simpleQueryLimit() {
        ReadTableRequest request = new ReadTableRequest("tableName").setLimit(5);

        assertThat(request.getReadCommand()).isEqualTo("SELECT * FROM tableName LIMIT 5");
    }

    @Test
    public void testGetReadCommand_simpleQueryOrderBy() {
        ReadTableRequest request =
                new ReadTableRequest("tableName")
                        .setOrderBy(new OrderByClause().addOrderByClause("column", true));

        assertThat(request.getReadCommand()).isEqualTo("SELECT * FROM tableName ORDER BY column");
    }

    @Test
    public void testGetReadCommand_simpleQueryFinalLimit() {
        ReadTableRequest request = new ReadTableRequest("tableName").setFinalLimit(5);

        assertThat(request.getReadCommand()).isEqualTo("SELECT * FROM tableName LIMIT 5");
    }

    @Test
    public void testGetReadCommand_simpleQueryFinalOrderBy() {
        ReadTableRequest request =
                new ReadTableRequest("tableName")
                        .setOrderBy(new OrderByClause().addOrderByClause("column", true));

        assertThat(request.getReadCommand()).isEqualTo("SELECT * FROM tableName ORDER BY column");
    }

    @Test
    public void testGetCountCommand_simpleQueryLimit() {
        ReadTableRequest request = new ReadTableRequest("tableName").setLimit(5);

        assertThat(request.getCountCommand()).isEqualTo("SELECT COUNT(*) FROM tableName LIMIT 5");
    }

    @Test
    public void testGetCountCommand_simpleQueryFinalLimit() {
        ReadTableRequest request = new ReadTableRequest("tableName").setLimit(5);

        assertThat(request.getCountCommand()).isEqualTo("SELECT COUNT(*) FROM tableName LIMIT 5");
    }

    @Test
    public void testGetReadCommand_oneColumn() {
        ReadTableRequest request = new ReadTableRequest("tableName").setColumnNames(List.of("col"));

        assertThat(request.getReadCommand()).isEqualTo("SELECT col FROM tableName");
    }

    @Test
    public void testGetCountCommand_oneColumn() {
        ReadTableRequest request = new ReadTableRequest("tableName").setColumnNames(List.of("col"));

        // SELECT COUNT(col) would only return a count of non-null rows, whereas the select would
        // return null rows, so COUNT(*) is needed.
        assertThat(request.getCountCommand()).isEqualTo("SELECT COUNT(*) FROM tableName");
    }

    @Test
    public void testGetReadCommand_multipleColumns() {
        ReadTableRequest request =
                new ReadTableRequest("tableName").setColumnNames(List.of("col1", "col2", "col3"));

        assertThat(request.getReadCommand()).isEqualTo("SELECT col1,col2,col3 FROM tableName");
    }

    @Test
    public void testGetCountCommand_multipleColumns() {
        ReadTableRequest request =
                new ReadTableRequest("tableName").setColumnNames(List.of("col1", "col2", "col3"));

        assertThat(request.getCountCommand()).isEqualTo("SELECT COUNT(*) FROM tableName");
    }

    @Test
    public void testGetReadCommand_distinctOneColumn() {
        ReadTableRequest request =
                new ReadTableRequest("tableName")
                        .setColumnNames(List.of("col"))
                        .setDistinctClause(true);

        assertThat(request.getReadCommand()).isEqualTo("SELECT DISTINCT col FROM tableName");
    }

    @Test
    public void testGetCountCommand_distinctOneColumn() {
        ReadTableRequest request =
                new ReadTableRequest("tableName")
                        .setColumnNames(List.of("col"))
                        .setDistinctClause(true);

        assertThat(request.getCountCommand())
                .isEqualTo("SELECT COUNT(DISTINCT col) FROM tableName");
    }

    @Test
    public void testGetReadCommand_distinctMultipleColumns() {
        ReadTableRequest request =
                new ReadTableRequest("tableName")
                        .setColumnNames(List.of("col1", "col2", "col3"))
                        .setDistinctClause(true);

        assertThat(request.getReadCommand())
                .isEqualTo("SELECT DISTINCT col1,col2,col3 FROM tableName");
    }

    @Test
    public void testGetCountCommand_distinctMultipleColumn() {
        ReadTableRequest request =
                new ReadTableRequest("tableName")
                        .setColumnNames(List.of("col1", "col2", "col3"))
                        .setDistinctClause(true);

        assertThat(request.getCountCommand())
                .isEqualTo("SELECT COUNT(DISTINCT col1,col2,col3) FROM tableName");
    }

    @Test
    public void testGetReadCommand_joinClauseWithLimitAndOrderBy() {
        ReadTableRequest request =
                new ReadTableRequest("tableName")
                        .setLimit(5)
                        .setOrderBy(new OrderByClause().addOrderByClause("columnName", true))
                        .setJoinClause(
                                new SqlJoin(
                                        "tableName",
                                        "otherTableName",
                                        "selfColumn",
                                        "otherColumn"));

        assertThat(request.getReadCommand())
                .isEqualTo(
                        "SELECT * FROM ( SELECT * FROM tableName ORDER BY columnName LIMIT 5 ) AS"
                                + " inner_query_result  INNER JOIN otherTableName ON"
                                + " inner_query_result.selfColumn = otherTableName.otherColumn");
    }

    @Test
    public void testGetReadCommand_joinClauseWithFinalLimitFinalAndOrderBy() {
        ReadTableRequest request =
                new ReadTableRequest("tableName")
                        .setFinalLimit(5)
                        .setFinalOrderBy(new OrderByClause().addOrderByClause("columnName", true))
                        .setJoinClause(
                                new SqlJoin(
                                        "tableName",
                                        "otherTableName",
                                        "selfColumn",
                                        "otherColumn"));

        assertThat(request.getReadCommand())
                .isEqualTo(
                        "SELECT * FROM ( SELECT * FROM tableName ) AS inner_query_result  INNER"
                                + " JOIN otherTableName ON inner_query_result.selfColumn ="
                                + " otherTableName.otherColumn ORDER BY columnName LIMIT 5");
    }

    @Test
    public void testGetReadCommand_joinClauseWithBothLimitsAndOrderBys() {
        ReadTableRequest request =
                new ReadTableRequest("tableName")
                        .setLimit(4)
                        .setFinalLimit(5)
                        .setOrderBy(new OrderByClause().addOrderByClause("columnName1", true))
                        .setFinalOrderBy(new OrderByClause().addOrderByClause("columnName2", true))
                        .setJoinClause(
                                new SqlJoin(
                                        "tableName",
                                        "otherTableName",
                                        "selfColumn",
                                        "otherColumn"));

        assertThat(request.getReadCommand())
                .isEqualTo(
                        "SELECT * FROM ( SELECT * FROM tableName ORDER BY columnName1 LIMIT 4 ) AS"
                                + " inner_query_result  INNER JOIN otherTableName ON"
                                + " inner_query_result.selfColumn = otherTableName.otherColumn "
                                + "ORDER BY"
                                + " columnName2 LIMIT 5");
    }

    @Test
    public void testGetReadCommand_unionQuery() {
        ReadTableRequest request = new ReadTableRequest("tableName").setColumnNames(List.of("col"));
        ReadTableRequest unionRequest =
                new ReadTableRequest("otherTableName").setColumnNames(List.of("col"));
        request.setUnionReadRequests(List.of(unionRequest));

        assertThat(request.getReadCommand())
                .isEqualTo(
                        "SELECT * FROM (SELECT col FROM otherTableName) UNION ALL SELECT col FROM"
                                + " tableName");
    }

    @Test
    public void testGetReadCommand_unionQueryWithLimitInBothQueries() {
        ReadTableRequest request =
                new ReadTableRequest("tableName").setColumnNames(List.of("col")).setLimit(2);
        ReadTableRequest unionRequest =
                new ReadTableRequest("otherTableName").setColumnNames(List.of("col")).setLimit(5);
        request.setUnionReadRequests(List.of(unionRequest));

        assertThat(request.getReadCommand())
                .isEqualTo(
                        "SELECT * FROM (SELECT col FROM otherTableName LIMIT 5) UNION ALL SELECT"
                                + " col FROM tableName LIMIT 2");
    }

    @Test
    public void testGetCountCommand_unionQuery() {
        ReadTableRequest request = new ReadTableRequest("tableName").setColumnNames(List.of("col"));
        ReadTableRequest unionRequest =
                new ReadTableRequest("otherTableName").setColumnNames(List.of("col"));
        request.setUnionReadRequests(List.of(unionRequest));

        assertThat(request.getCountCommand())
                .isEqualTo(
                        "SELECT COUNT(*) FROM (SELECT * FROM (SELECT col FROM otherTableName) UNION"
                                + " ALL SELECT col FROM tableName)");
    }

    @Test
    public void testGetReadCommand_joinClauseWithPostJoinWhereClause() {
        WhereClauses finalWhereClause = new WhereClauses(WhereClauses.LogicalOperator.AND);
        finalWhereClause.addWhereEqualsClause("someOtherColumn", "someValue");
        ReadTableRequest request =
                new ReadTableRequest("tableName")
                        .setJoinClause(
                                new SqlJoin(
                                        "tableName", "otherTableName", "selfColumn", "otherColumn"))
                        .setPostJoinWhereClause(finalWhereClause);

        assertThat(request.getReadCommand())
                .isEqualTo(
                        "SELECT * FROM ( SELECT * FROM tableName ) AS inner_query_result  INNER "
                                + "JOIN otherTableName ON inner_query_result.selfColumn = "
                                + "otherTableName.otherColumn WHERE someOtherColumn = 'someValue'");
    }

    @Test
    public void testGetReadCommand_joinClauseWithBothWhereClauses() {
        WhereClauses whereClause = new WhereClauses(WhereClauses.LogicalOperator.AND);
        whereClause.addWhereEqualsClause("someColumn", "someValue");
        WhereClauses finalWhereClause = new WhereClauses(WhereClauses.LogicalOperator.AND);
        finalWhereClause.addWhereEqualsClause("someOtherColumn", "otherValue");
        ReadTableRequest request =
                new ReadTableRequest("tableName")
                        .setWhereClause(whereClause)
                        .setJoinClause(
                                new SqlJoin(
                                        "tableName", "otherTableName", "selfColumn", "otherColumn"))
                        .setPostJoinWhereClause(finalWhereClause);

        assertThat(request.getReadCommand())
                .isEqualTo(
                        "SELECT * FROM ( SELECT * FROM tableName WHERE someColumn = 'someValue' )"
                                + " AS inner_query_result  INNER JOIN otherTableName ON "
                                + "inner_query_result.selfColumn = otherTableName.otherColumn "
                                + "WHERE someOtherColumn = 'otherValue'");
    }

    @Test
    public void testGetReadCommand_noJoinClauseWithBothWhereClauses() {
        WhereClauses whereClause = new WhereClauses(WhereClauses.LogicalOperator.AND);
        whereClause.addWhereEqualsClause("someColumn", "someValue");
        WhereClauses finalWhereClause = new WhereClauses(WhereClauses.LogicalOperator.AND);
        finalWhereClause.addWhereEqualsClause("someOtherColumn", "otherValue");
        ReadTableRequest request =
                new ReadTableRequest("tableName")
                        .setWhereClause(whereClause)
                        .setPostJoinWhereClause(finalWhereClause);

        assertThat(request.getReadCommand())
                .isEqualTo("SELECT * FROM tableName WHERE someColumn = 'someValue'");
    }
}
