package com.httprunnerjava.builtin;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.httprunnerjava.utils.JsonUtils;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.List;
import java.util.Objects;

/**
 * 所有比对不一致产生的差异，都要尽量生成AssertionError类型的错误，这样才可以被上层捕获，被认为是比对造成的差异，否则会被作为其他异常类型捕获
 *
 * @param <T>
 */
@Slf4j
public class Comparator<T> {
    private final Class<?> cls;

    public Comparator(T t1) {
        cls = t1.getClass();
    }

    public void showValue(T checkalue, T expectValue) {
        try {
            Allure.addAttachment("实际值", String.valueOf(checkalue));
        } catch (Exception e) {
            log.error("添加实际值到allure报告中失败，失败原因是：" + e.getMessage());
        }
        try {
            Allure.addAttachment("期望值", String.valueOf(expectValue));
        } catch (Exception e) {
            log.error("添加期望值到allure报告中失败，失败原因是：" + e.getMessage());
        }
    }

    /**
     * 用于判断两个值是否相等
     *
     * @param checkalue   实际值
     * @param expectValue 期望值
     */
    public void objectEquals(T checkalue, T expectValue) {
        if (!Objects.equals(checkalue, expectValue)) {
            showValue(checkalue, expectValue);
            throw new AssertionError("比对结果与预期不一致");
        }
    }

    /**
     * 用于判断两个值的类型是否相等
     *
     * @param checkalue   实际值
     * @param expectValue 期望值
     */
    public void typeMatch(T checkalue, T expectValue) {
        //TODO:未实现，原版httprunner中实现了该方法，但是实际用下来好像没遇到此场景
    }

    /**
     * 用于判断两个值的大小
     *
     * @param checkalue   实际值
     * @param expectValue 期望值
     */
    public void lessThan(T checkalue, T expectValue) {
        showValue(checkalue, expectValue);
        if (checkalue instanceof Integer) {
            assert (Integer) checkalue < (Integer) expectValue;
        } else if (checkalue instanceof Double) {
            //TODO: int 和 double 有没有大于小于的比较？
            assert (Double) checkalue < (Double) expectValue;
        } else {
            throw new AssertionError("比对结果与预期不一致");
        }
    }

    /**
     * 用于判断实际值是否包含期望值
     *
     * @param checkalue   实际值
     * @param expectValue 期望值
     */
    public void listContains(T checkalue, Object expectValue) {
        showValue(checkalue, (T) expectValue);
        if (checkalue instanceof JSONArray) {
            if (expectValue instanceof String) {
                JSONArray expectValueArray = JSON.parseArray(expectValue.toString());
                JsonUtils.containJsonArray((JSONArray) checkalue, expectValueArray, null);
            }
        } else {
            throw new AssertionError("比对结果与预期不一致");
        }
    }

    public void listNotContains(T checkalue, Object expectValue) {
        showValue(checkalue, (T) expectValue);
        if (checkalue instanceof JSONArray) {
            if (expectValue instanceof String) {
                JSONArray expectValueArray = JSON.parseArray(expectValue.toString());
                JsonUtils.notContainJsonArray((JSONArray) checkalue, expectValueArray, null);
            }
        } else {
            throw new AssertionError("比对结果与预期不一致");
        }
    }

    public void listSize(T checkalue, Object expectValue) {
        showValue(checkalue, (T) expectValue);
        if (checkalue instanceof JSONArray) {
            if (!expectValue.equals(((JSONArray) checkalue).size())) {
                log.error("校验的list与预期size不一致");
                throw new AssertionError("比对结果与预期不一致");
            }
        } else {
            throw new AssertionError("比对结果与预期不一致");
        }
    }

    public void listEmpty(T checkalue, Object expectValue) {
        showValue(checkalue, (T) expectValue);
        if (checkalue instanceof List) {
            if (!(((List) checkalue).size() == 0)) {
                throw new AssertionError("比对结果与预期不一致");
            }
        } else {
            throw new AssertionError("比对结果与预期不一致");
        }
    }

    public void jsonEquals(Object checkalue, Object expectValue) {
        showValue((T) checkalue, (T) expectValue);
        //这里有个顺序的调换，需要注意
        JsonUtils.compareJson(expectValue.toString(), checkalue.toString());
    }

    public void jsonEqualsWithStrictMode(Object checkalue, Object expectValue) {
        showValue((T) checkalue, (T) expectValue);
        //这里有个顺序的调换，需要注意
        JsonUtils.compareJson(expectValue.toString(), checkalue.toString(), JSONCompareMode.STRICT);
    }

    public void jsonEqualsWithStrictOrderMode(Object checkalue, Object expectValue) {
        showValue((T) checkalue, (T) expectValue);
        //这里有个顺序的调换，需要注意
        JsonUtils.compareJson(expectValue.toString(), checkalue.toString(), JSONCompareMode.STRICT_ORDER);
    }

    public void jsonEqualsWithNonExtensibleMode(Object checkalue, Object expectValue) {
        showValue((T) checkalue, (T) expectValue);
        //这里有个顺序的调换，需要注意
        JsonUtils.compareJson(expectValue.toString(), checkalue.toString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    public void jsonNotEquals(Object checkalue, Object expectValue) {
        showValue((T) checkalue, (T) expectValue);
        //这里有个顺序的调换，需要注意
        JsonUtils.compareJson(expectValue.toString(), checkalue.toString());

        throw new AssertionError();
    }

    public void notListEmpty(T checkalue, Object expectValue) {
        showValue((T) checkalue, (T) expectValue);
        if (checkalue instanceof List) {
            if ((((List) checkalue).size() == 0)) {
                throw new AssertionError("比对结果与预期不一致");
            }
        } else {
            throw new AssertionError("比对结果与预期不一致");
        }
    }
}
