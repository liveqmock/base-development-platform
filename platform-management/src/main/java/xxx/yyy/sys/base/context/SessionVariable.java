/**
 * Copyright (C) 2014 serv (liuyuhua69@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xxx.yyy.sys.base.context;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import xxx.yyy.framework.common.application.SpringContextHolder;
import xxx.yyy.sys.datafilter.context.FilterContext;
import xxx.yyy.sys.datafilter.model.FilterRule;
import xxx.yyy.sys.datafilter.service.FilterRuleService;
import xxx.yyy.sys.rbac.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 * 用户上下文模型实体
 *
 * @author serv
 *
 */
public class SessionVariable implements FilterContext{


    // 当前用户
    private Map<String,Object> contextList = Maps.newHashMap();

    //用户具有的所有规则权限list
    private List<FilterRule> filterRuleList;


    public SessionVariable(User user) {
        this.put(DepartmentContext.class.getName(),new DepartmentContext(user));
        this.put(PostContext.class.getName(),new PostContext(user));
        this.put(ResourceContext.class.getName(),new ResourceContext(user));
        this.put(RoleContext.class.getName(),new RoleContext(user));
        this.put(GroupContext.class.getName(),new GroupContext(user));
        this.put(OrgContext.class.getName(),new OrgContext(user));
        //初始化用户各种上下文信息后，开始初始化用户权限
        initRules();
    }

    private void initRules(){
        List<String> relationIds = Lists.newArrayList(getUserId());//添加当前用户id
        relationIds.addAll(getRoleContext().getRoleIds());//添加所属角色ids
        relationIds.addAll(getDepartmentContext().getDepartmentIds());//添加部门ids
        relationIds.addAll(getPostContext().getPostIds());//添加岗位ids
        relationIds.addAll(getGroupContext().getGroupIds());//添加群组ids
        //获取用户具有的所有规则权限list
        filterRuleList = SpringContextHolder.getBean(FilterRuleService.class).queryUnDeleted().findAll("x.state = 1 and ( "+applyRelationIds(relationIds)+" )");
    }

    private String applyRelationIds(List<String> relationIds){
        Collection<String> newRelationIds = Collections2.transform(relationIds,new Function<String, String>() {
            @Override
            public String apply(String input) {
                return "x.relationId = '"+input+"'";
            }
        });
        return StringUtils.join(newRelationIds," or ");
    }

    /**
     * 获取当前登录用户ID
     * @return
     */
    public String getUserId(){
        return getUser().getId();
    }

    /**
     * 获取当前登录用户
     * @return
     */
    public User getUser(){
        return get(DepartmentContext.class.getName()).getUser();
    }

    /**
     * 获取机构、部门、群组 上下文
     * @return
     */
    public DepartmentContext getDepartmentContext(){
        return get(DepartmentContext.class.getName());
    }

    /**
     * 获取岗位上下文
     * @return
     */
    public PostContext getPostContext(){
        return get(PostContext.class.getName());
    }

    /**
     * 获取用户资源上下文
     * @return
     */
    public ResourceContext getResourceContext(){
        return get(ResourceContext.class.getName());
    }

    /**
     * 获取角色上下文
     * @return
     */
    public RoleContext getRoleContext(){
        return get(RoleContext.class.getName());
    }

    /**
     * 获取用户单位上下文
     * @return
     */
    public OrgContext getOrgContext(){
        return get(OrgContext.class.getName());
    }


    /**
     * 获取用户群组
     * @return
     */
    public GroupContext getGroupContext(){
        return get(GroupContext.class.getName());
    }


    private <T extends AbstractUserContext> void put(String key,T t){
        contextList.put(key,t);
    }

    private <T extends AbstractUserContext> T get(String key){
        return (T) contextList.get(key);
    }


    /**
     * 获取用户具有的规则权限的jpql 列表
     *
     * @param modelClass     操作的entityClass
     * @param dataFilterType 数据过滤类型 {@link xxx.yyy.sys.datafilter.DataFilterType}
     * @return jpql 列表
     */
    public Collection<String> getFilterRuleJpqlList(final Class modelClass, final String dataFilterType , final String orgId) {
        Collection<String> result = Collections2.transform(filterRuleList,new Function<FilterRule, String>() {
            @Override
            public String apply(FilterRule input) {
                if (StringUtils.equals(modelClass.getName(), input.getModelClassName())) {
                    if (StringUtils.equals(dataFilterType, input.getOperationType())) {
                        if(StringUtils.isBlank(orgId)){//如果为null 则不过滤单位
                            return "("+input.getConditionJpql()+")";
                        }
                        if(StringUtils.equals(input.getOrgId(),orgId)){
                            return "("+input.getConditionJpql()+")";
                        }
                    }
                }
                return null;
            }
        });
        return Collections2.filter(result, Predicates.notNull());
    }



    /**
     * 获取用户的变量map信息
     *
     * @return
     */
    public Map<String,Object> getFilterParameters(){
        Map<String,Object> filterParameters = Maps.newHashMap();
        filterParameters.put(USER_ID, getUser().getId());
        filterParameters.put(USER_ORG_ID, getOrgContext().getOrgId());
        filterParameters.put(USER_PARENT_DEPT_IDS, getDepartmentContext().getContainsParentIds());
        filterParameters.put(USER_DEPT_IDS, getDepartmentContext().getContainsParentIds());
        filterParameters.put(USER_GROUP_IDS, getGroupContext().getGroupIds());
        filterParameters.put(USER_DEFAULT_DEPT_ID, getDepartmentContext().getDefaultDeptId());
        filterParameters.put(USER_CHILD_DEPT_IDS, getDepartmentContext().getChildDepartmentIds());
        filterParameters.put(USER_CHILD_ORG_IDS, getOrgContext().getChildOrgIds());
        filterParameters.put(USER_SECRET_LEVEL,getUser().getSecretLevel());
        filterParameters.put(DATE_NOW,new DateTime().toDate());
        filterParameters.put(DATE_YEAR,new DateTime().getYear());
        return filterParameters;
    }



}
