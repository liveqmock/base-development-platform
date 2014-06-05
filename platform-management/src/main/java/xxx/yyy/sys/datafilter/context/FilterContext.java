package xxx.yyy.sys.datafilter.context;

import java.util.List;
import java.util.Map;

/**
 * Created by serv on 14-6-5.
 */
public interface FilterContext {

    /**
     * 获取用户具有的规则权限的jpql 列表
     *
     * @param modelClass     操作的entityClass
     * @param dataFilterType 数据过滤类型 {@link xxx.yyy.sys.datafilter.DataFilterType}
     * @return jpql 列表
     */
    public List<String> getFilterRuleJpqlList(final Class modelClass, final String dataFilterType);

    /**
     * 获取用户的变量map信息
     *
     * @return
     */
    public Map<String,Object> getFilterParameters();

}