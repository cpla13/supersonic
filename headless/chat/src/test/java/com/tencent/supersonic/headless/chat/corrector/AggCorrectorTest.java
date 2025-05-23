package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class AggCorrectorTest {

    @Test
    void testDoCorrect() {
        AggCorrector corrector = new AggCorrector();
        Long dataSetId = 1L;
        ChatQueryContext chatQueryContext = buildQueryContext(dataSetId);
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        SchemaElement dataSet = new SchemaElement();
        dataSet.setDataSetId(dataSetId);
        semanticParseInfo.setDataSet(dataSet);
        SqlInfo sqlInfo = new SqlInfo();
        String sql = "SELECT 用户, 访问次数 FROM 超音数数据集 WHERE 部门 = 'sales' AND"
                + " datediff('day', 数据日期, '2024-06-04') <= 7"
                + " GROUP BY 用户 ORDER BY SUM(访问次数) DESC LIMIT 1";
        sqlInfo.setParsedS2SQL(sql);
        sqlInfo.setCorrectedS2SQL(sql);
        semanticParseInfo.setSqlInfo(sqlInfo);
        corrector.correct(chatQueryContext, semanticParseInfo);
        Assert.assertEquals(
                "SELECT 用户, SUM(访问次数) FROM 超音数数据集 WHERE 部门 = 'sales'"
                        + " AND datediff('day', 数据日期, '2024-06-04') <= 7 GROUP BY 用户"
                        + " ORDER BY SUM(访问次数) DESC LIMIT 1",
                semanticParseInfo.getSqlInfo().getCorrectedS2SQL());
    }

    @Test
    void testSchemaCorrector() {
        SchemaCorrector corrector = new SchemaCorrector();
        Long dataSetId = 1L;
        ChatQueryContext chatQueryContext = buildQueryContext(dataSetId);
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        SchemaElement dataSet = new SchemaElement();
        dataSet.setDataSetId(dataSetId);
        semanticParseInfo.setDataSet(dataSet);
        SqlInfo sqlInfo = new SqlInfo();
        String sql =
                "WITH 总停留时长 AS (SELECT 用户, SUM(停留时长) AS _总停留时长_ FROM 超音数数据集 WHERE 用户 IN ('alice', 'lucy') AND 数据日期 >= '2025-03-01' AND 数据日期 <= '2025-03-12' GROUP BY 用户) SELECT 用户, _总停留时长_ FROM 总停留时长";
        sqlInfo.setParsedS2SQL(sql);
        sqlInfo.setCorrectedS2SQL(sql);
        semanticParseInfo.setSqlInfo(sqlInfo);
        corrector.correct(chatQueryContext, semanticParseInfo);
        Assert.assertEquals(
                "WITH 总停留时长 AS (SELECT 用户名, SUM(停留时长) AS _总停留时长_ FROM 超音数数据集 WHERE 用户名 IN ('alice', 'lucy') AND 数据日期 "
                        + ">= '2025-03-01' AND 数据日期 <= '2025-03-12' GROUP BY 用户名) SELECT 用户名, _总停留时长_ FROM 总停留时长",
                semanticParseInfo.getSqlInfo().getCorrectedS2SQL());
    }

    private ChatQueryContext buildQueryContext(Long dataSetId) {
        ChatQueryContext chatQueryContext = new ChatQueryContext();
        List<DataSetSchema> dataSetSchemaList = new ArrayList<>();
        DataSetSchema dataSetSchema = new DataSetSchema();
        QueryConfig queryConfig = new QueryConfig();
        dataSetSchema.setQueryConfig(queryConfig);
        SchemaElement schemaElement = new SchemaElement();
        schemaElement.setDataSetId(dataSetId);
        dataSetSchema.setDataSet(schemaElement);
        Set<SchemaElement> dimensions = new HashSet<>();

        dimensions.add(SchemaElement.builder().dataSetId(1L).name("部门").build());

        dimensions.add(SchemaElement.builder().dataSetId(1L).name("用户名").build());

        dataSetSchema.setDimensions(dimensions);

        Set<SchemaElement> metrics = new HashSet<>();

        metrics.add(SchemaElement.builder().dataSetId(1L).name("访问次数").build());

        metrics.add(SchemaElement.builder().dataSetId(1L).name("停留时长").build());

        dataSetSchema.setMetrics(metrics);
        dataSetSchemaList.add(dataSetSchema);

        SemanticSchema semanticSchema = new SemanticSchema(dataSetSchemaList);
        chatQueryContext.setSemanticSchema(semanticSchema);
        return chatQueryContext;
    }
}
