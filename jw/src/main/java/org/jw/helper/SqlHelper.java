package org.jw.helper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jw.annotation.Id;
import org.jw.annotation.Table;
import org.jw.bean.EntityFieldMethod;
import org.jw.bean.SqlPackage;
import org.jw.util.CastUtil;
import org.jw.util.CollectionUtil;
import org.jw.util.JsonUtil;
import org.jw.util.ReflectionUtil;
import org.jw.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sql 接卸 助手类
 * 剥离自DataBaseHelper
 * Created by CaiDongYu on 2016/5/6.
 */
public class SqlHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(SqlHelper.class);

	public static Map<String,Class<?>> matcherEntityClassMap(String sql){
		Map<String, Class<?>> entityClassMap = TableHelper.getEntityClassMap();
    	String sqlClean = sql.replaceAll("[,><=!\\+\\-\\*/\\(\\)]", " ");
    	String[] sqlWords = sqlClean.split(" ");
    	LOGGER.debug("sqlWords : "+Arrays.toString(sqlWords));
    	
    	Map<String,Class<?>> sqlEntityClassMap = new HashMap<>();
    	for(String word:sqlWords){ 
    		if(entityClassMap.containsKey(word) && !sqlEntityClassMap.containsKey(word)){
    			sqlEntityClassMap.put(word, entityClassMap.get(word));
    		}
    	}
    	return sqlEntityClassMap;
    }
	
	public static SqlPackage getInsertSqlPackage(Object entity){
		String sql = "INSERT INTO " + TableHelper.getTableName(entity.getClass());
        List<EntityFieldMethod> entityFieldMethodList = ReflectionUtil.getGetMethodList(entity.getClass());
        StringBuilder columns = new StringBuilder("(");
        StringBuilder values = new StringBuilder("(");
        Object[] params = new Object[entityFieldMethodList.size()];
        for (int i=0;i<entityFieldMethodList.size();i++) {
        	EntityFieldMethod entityFieldMethod = entityFieldMethodList.get(i);
        	Field field = entityFieldMethod.getField();
        	Method method = entityFieldMethod.getMethod();
        	String column = StringUtil.camelToUnderline(field.getName());
            columns.append(column).append(", ");
            values.append("?, ");
            params[i] = ReflectionUtil.invokeMethod(entity, method);
        }
        columns.replace(columns.lastIndexOf(", "), columns.length(), ")");
        values.replace(values.lastIndexOf(", "), values.length(), ")");
        sql += columns + " VALUES " + values;
        return new SqlPackage(sql, params);
	}
	
	public static SqlPackage getUpdateSqlPackage(Object entity){
		String sql = "UPDATE " + TableHelper.getTableName(entity.getClass()) + " SET ";
        List<EntityFieldMethod> entityFieldMethodList = ReflectionUtil.getGetMethodList(entity.getClass());
        //#会做修改的字段
        StringBuilder columns = new StringBuilder();
        //#修改的条件
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        //#占位符的值
        Object[] params = new Object[entityFieldMethodList.size()];
        boolean hashIdField = false;
        for (int i=0;i<entityFieldMethodList.size();i++) {
        	EntityFieldMethod entityFieldMethod = entityFieldMethodList.get(i);
        	Field field = entityFieldMethod.getField();
        	Method method = entityFieldMethod.getMethod();
        	String column = StringUtil.camelToUnderline(field.getName());
        	if(!field.isAnnotationPresent(Id.class)){
        		//#没有@Id注解的字段作为修改内容
        		columns.append(column).append("=?, ");
        	}else{
        		//#有@Id的字段作为主键，用来当修改条件
        		where.append(" and "+column+"=?");
        		hashIdField = true;
        	}
        	params[i] = ReflectionUtil.invokeMethod(entity, method);
        }
        columns.replace(columns.lastIndexOf(", "), columns.length(), " ");
        sql = sql+columns.toString()+where.toString();

        if(!hashIdField){
        	//注意，updateEntity，如果Entity中没有标注@Id的字段，是不能更新的，否则会where 1=1 全表更新！
        	throw new RuntimeException("update entity failure!cannot find any field with @Id in "+entity.getClass());
        }
        return new SqlPackage(sql, params);
	}
	
	
	public static SqlPackage getInsertOnDuplicateKeyUpdateSqlPackage(Object entity){
		String sql = "INSERT INTO " + TableHelper.getTableName(entity.getClass());
        List<EntityFieldMethod> entityFieldMethodList = ReflectionUtil.getGetMethodList(entity.getClass());
        //#字段
        StringBuilder columnsInsert = new StringBuilder("(");
        StringBuilder valuesInsert = new StringBuilder(" VALUES (");
        StringBuilder columnsUpdate = new StringBuilder(" ON DUPLICATE KEY UPDATE ");
        //#占位符的值
        List<Object> params = new ArrayList<>();
        boolean hashIdField = false;
        for (int i=0;i<entityFieldMethodList.size();i++) {
        	//# insert
        	EntityFieldMethod entityFieldMethod = entityFieldMethodList.get(i);
        	Field field = entityFieldMethod.getField();
        	Method method = entityFieldMethod.getMethod();
        	String column = StringUtil.camelToUnderline(field.getName());
        	
        	columnsInsert.append(column).append(", ");
        	valuesInsert.append("?, ");
        	params.add(ReflectionUtil.invokeMethod(entity, method));
        }
        columnsInsert.replace(columnsInsert.lastIndexOf(", "), columnsInsert.length(), ")");
        valuesInsert.replace(valuesInsert.lastIndexOf(", "), valuesInsert.length(), ")");
        for (int i=0;i<entityFieldMethodList.size();i++) {
        	//# update
        	EntityFieldMethod entityFieldMethod = entityFieldMethodList.get(i);
        	Field field = entityFieldMethod.getField();
        	Method method = entityFieldMethod.getMethod();
        	String column = StringUtil.camelToUnderline(field.getName());
        	
        	//# update
        	if(!field.isAnnotationPresent(Id.class)){
        		//#没有@Id注解的字段作为修改内容
        		columnsUpdate.append(column).append("=?, ");
        		params.add(ReflectionUtil.invokeMethod(entity, method));
        	}else{
        		hashIdField = true;
        	}
        }
        columnsUpdate.replace(columnsUpdate.lastIndexOf(", "), columnsUpdate.length(), " ");

        sql = sql+columnsInsert.toString()+valuesInsert.toString()+columnsUpdate.toString();
        
        if(!hashIdField){
        	//注意，updateEntity，如果Entity中没有标注@Id的字段，是不能更新的，否则会where 1=1 全表更新！
        	throw new RuntimeException("update entity failure!cannot find any field with @Id in "+entity.getClass());
        }
        return new SqlPackage(sql, params.toArray());
	}
	
	public static SqlPackage getDeleteSqlPackage(Object entity){
		String sql = "DELETE FROM " + TableHelper.getTableName(entity.getClass());
        List<EntityFieldMethod> entityFieldMethodList = ReflectionUtil.getGetMethodList(entity.getClass());
        //#修改的条件
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        //#占位符的值
        //#先过滤出带有@Id的EntityFieldMethod
        List<EntityFieldMethod> idFieldList = entityFieldMethodList.stream().filter(
        		entityFieldMethod->entityFieldMethod.getField().isAnnotationPresent(Id.class)
        		).collect(Collectors.toList());
        Object[] params = new Object[idFieldList.size()];
        for (int i=0;i<idFieldList.size();i++) {
        	EntityFieldMethod entityFieldMethod = idFieldList.get(i);
        	Field field = entityFieldMethod.getField();
        	Method method = entityFieldMethod.getMethod();
        	String column = StringUtil.camelToUnderline(field.getName());
        	//#有@Id的字段作为主键，用来当修改条件
    		where.append(" and "+column+"=?");
    		params[i] = ReflectionUtil.invokeMethod(entity, method);
        }
        sql = sql+where.toString();
        
        if(CollectionUtil.isEmpty(idFieldList)){
        	//注意，deleteEntity，如果Entity中没有标注@Id的字段，是不能删除的，否则会where 1=1 全表删除！
        	throw new RuntimeException("delete entity failure!cannot find any field with @Id in "+entity.getClass());
        }
        return new SqlPackage(sql, params);
	}
	
	public static SqlPackage getSelectSqlPackage(Object entity){
		String sql = "SELECT * FROM " + TableHelper.getTableName(entity.getClass());
        List<EntityFieldMethod> entityFieldMethodList = ReflectionUtil.getGetMethodList(entity.getClass());
        //#修改的条件
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        //#占位符的值
        //#先过滤出带有@Id的EntityFieldMethod
        List<EntityFieldMethod> idFieldList = entityFieldMethodList.stream().filter(
        		entityFieldMethod->entityFieldMethod.getField().isAnnotationPresent(Id.class)
        		).collect(Collectors.toList());
        Object[] params = new Object[idFieldList.size()];
        for (int i=0;i<idFieldList.size();i++) {
        	EntityFieldMethod entityFieldMethod = idFieldList.get(i);
        	Field field = entityFieldMethod.getField();
        	Method method = entityFieldMethod.getMethod();
        	String column = StringUtil.camelToUnderline(field.getName());
        	//#有@Id的字段作为主键，用来当修改条件
    		where.append(" and "+column+"=?");
    		params[i] = ReflectionUtil.invokeMethod(entity, method);
        }
        sql = sql+where.toString();
        
        if(CollectionUtil.isEmpty(idFieldList)){
        	//注意，deleteEntity，如果Entity中没有标注@Id的字段，是不能删除的，否则会where 1=1 全表删除！
        	throw new RuntimeException("select entity failure!cannot find any field with @Id in "+entity.getClass());
        }
        
        return new SqlPackage(sql, params);
	}
	
	
	/**
     * 解析sql中的类信息
     */
    public static String convertHql2Sql(String sql){
    	//末尾多加一个空格，防止select * from table这样的bug，会找不到表名
    	sql = sql+" ";
    	LOGGER.debug("sql : "+sql);
    	//#根据sql匹配出Entity类
    	Map<String,Class<?>> sqlEntityClassMap = matcherEntityClassMap(sql);
    	LOGGER.debug("sqlEntityClassMap : "+JsonUtil.toJson(sqlEntityClassMap));
    	//#解析表名
    	sql = convertTableName(sql,sqlEntityClassMap);
    	//#解析字段
    	sql = convertColumnName(sql, sqlEntityClassMap);
    	return sql;
    }
    
    
    
    private static String convertTableName(String sql, Map<String,Class<?>> sqlEntityClassMap){
    	//#表名可能被这些东西包围，空格本身就用来分割，所以不算在内
    	for(Map.Entry<String, Class<?>> sqlEntityClassEntry:sqlEntityClassMap.entrySet()){
    		String entityClassSimpleName = sqlEntityClassEntry.getKey();
    		Class<?> entityClass = sqlEntityClassEntry.getValue();
    		Table tableAnnotation = entityClass.getAnnotation(Table.class);
    		//#替换表名
    		//这里的表达式就需要空格了
    		String tableNameReg = "([,><=!\\+\\-\\*/\\(\\) ])"+entityClassSimpleName+"([,><=!\\+\\-\\*/\\(\\) ])";
    		Pattern p = Pattern.compile(tableNameReg);
    		Matcher m = p.matcher(sql);
    		while(m.find()){//这就可以找到表名，包括表名前后的字符，后面替换的时候，就能很方便替换了
    			String tablePre = m.group(1);
    			String tableAfter = m.group(2);
    			if("+-*()".contains(tablePre)){
    				tablePre = "\\"+tablePre;
    			}
    			if("+-*()".contains(tableAfter)){
    				tableAfter = "\\"+tableAfter;
    			}
    			tableNameReg = tablePre+entityClassSimpleName+tableAfter;
    			String tableNameAround = tablePre+tableAnnotation.value()+tableAfter;
    			sql = sql.replaceAll(tableNameReg, tableNameAround);
    		}
    	}
    	
		return sql;
    }
    
    private static String convertColumnName(String sql, Map<String,Class<?>> sqlEntityClassMap){

    	for(Map.Entry<String, Class<?>> sqlEntityClassEntry:sqlEntityClassMap.entrySet()){
    		Class<?> entityClass = sqlEntityClassEntry.getValue();
    		List<EntityFieldMethod> entityFieldMethodList = ReflectionUtil.getGetMethodList(entityClass);
        	//#根据get方法来解析字段名
        	for(EntityFieldMethod entityFieldMethod:entityFieldMethodList){
    			String field = entityFieldMethod.getField().getName();
    			String column = StringUtil.camelToUnderline(field);
    			//前后表达式不同
    			//前面有! 
    			//前面有(
    			//前面有.
    			//后面有)
    			String columnNameReg = "([,><=\\+\\-\\*/\\(\\. ])"+field+"([,><=!\\+\\-\\*/\\) ])";
        		Pattern p = Pattern.compile(columnNameReg);
        		Matcher m = p.matcher(sql);
        		while(m.find()){//这就可以找到表名，包括表名前后的字符，后面替换的时候，就能很方便替换了
        			String columnPre = m.group(1);
        			String columnAfter = m.group(2);
        			if("+-*(.".contains(columnPre)){
        				columnPre = "\\"+columnPre;
        			}
        			if("+-*)".contains(columnAfter)){
        				columnAfter = "\\"+columnAfter;
        			}
        			columnNameReg = columnPre+field+columnAfter;
        			String columnNameAround = columnPre+column+columnAfter;
        			sql = sql.replaceAll(columnNameReg, columnNameAround);
        		}
        	}
    	}
    	
    	return sql;
    }
    
    /**
     * 转换占位符 ?1
     */
    public static SqlPackage convertGetFlag(String sql,Object[] params){
    	//#检测占位符是否都符合格式
    	//?后面跟1~9,如果两位数或者更多位,则十位开始可以0~9
    	//但是只用检测个位就好
    	int getFlagIndex = sql.indexOf("?");
    	boolean getFlagComm = false;//普通模式 就是?不带数字
    	boolean getFlagSpec = false;//?带数字模式
    	while(getFlagIndex >= 0 && getFlagIndex<sql.length()-1){
    		char c = sql.charAt(getFlagIndex+1);
    		if(c < '1' || c > '9'){
    			getFlagComm = true;
    		}else{
    			getFlagSpec = true;
    		}
    		getFlagIndex = sql.indexOf("?", getFlagIndex+1);
    	}
    	if(sql.trim().endsWith("?"))
    		getFlagComm = true;
    	
    	//不可以两种模式都并存，只能选一种，要么?都带数字，要么?都不带数字
    	if(getFlagComm && getFlagSpec)
			throw new RuntimeException("invalid sql statement with ?+number and only ?: ");
    	
    	//#开始排布
    	//* 根据sql中?1这样的取值占位符，
    	List<Object> paramList = new ArrayList<>();
    	if(getFlagComm){
    		getFlagIndex = sql.indexOf("?");
    		int paramIndex = 0;
    		while(getFlagIndex >= 0 && getFlagIndex<sql.length()){
        		Object param = params[paramIndex++];
        		if(param != null){
        			if(param.getClass().isArray()){
            			throw new RuntimeException("invalid sql param is arry: "+param);
            		}
            		if(List.class.isAssignableFrom(param.getClass())){
            			StringBuffer getFlagReplaceBuffer = new StringBuffer();
            			List<?> param2List = (List<?>)param;//这就是针对 in 操作的
            			if(CollectionUtil.isEmpty(param2List)){
            	    		throw new RuntimeException("invalid sql param is null or empty: "+param);
            			}
            			//把元素都取出来，追加到新的元素列表里
            			for(Object eachParam:param2List){
            				paramList.add(eachParam);
            				//有一个元素，就有一个占位符
            				getFlagReplaceBuffer.append("?, ");
            			}
            			getFlagReplaceBuffer.replace(getFlagReplaceBuffer.lastIndexOf(", "), getFlagReplaceBuffer.length(), "");
            			String sql1 = sql.substring(0,getFlagIndex);
            			String sql2 = sql.substring(getFlagIndex+1);
            			sql = sql1+getFlagReplaceBuffer.toString()+sql2;
            			getFlagIndex = getFlagIndex+getFlagReplaceBuffer.length()-1;
            		}else{
            			paramList.add(param);
            		}
        		}else{
        			paramList.add(param);
        		}
        		
    			getFlagIndex = sql.indexOf("?", getFlagIndex+1);
        	}
    	}else if(getFlagSpec){
    		Pattern p = Pattern.compile("\\?([1-9][0-9]*)");
        	Matcher m = p.matcher(sql);
        	while(m.find()){
        		String getFlagNumber = m.group(1);
        		int paramIndex = CastUtil.castInteger(getFlagNumber)-1;
        		
        		Object param = params[paramIndex];
        		StringBuffer getFlagReplaceBuffer = new StringBuffer();
        		if(param != null){
        			if(param.getClass().isArray()){
            			throw new RuntimeException("invalid sql param is arry: "+param);
            		}
            		
            		if(List.class.isAssignableFrom(param.getClass())){
            			List<?> param2List = (List<?>)param;//这就是针对 in 操作的
            			if(CollectionUtil.isEmpty(param2List)){
            	    		throw new RuntimeException("invalid sql param is null or empty: "+param);
            			}
            			//把元素都取出来，追加到新的元素列表里
            			for(Object eachParam:param2List){
            				paramList.add(eachParam);
            				//有一个元素，就有一个占位符
            				getFlagReplaceBuffer.append("?, ");
            			}
            			getFlagReplaceBuffer.replace(getFlagReplaceBuffer.lastIndexOf(", "), getFlagReplaceBuffer.length(), "");
            		}else{
            			getFlagReplaceBuffer.append("?");
            			paramList.add(param);
            		}
        		}else{
        			getFlagReplaceBuffer.append("?");
        			paramList.add(param);
        		}
        		
        		//把sql中对应的?1变成?或者?,?,?
        		sql = sql.replace("?"+getFlagNumber, getFlagReplaceBuffer.toString());
        	}
    	}
    	
    	
    	return new SqlPackage(sql, paramList.toArray());
    }
}