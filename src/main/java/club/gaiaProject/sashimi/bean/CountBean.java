package club.gaiaProject.sashimi.bean;

/**
 * Created by luoxiaolong on 18-5-11.
 */
public class CountBean {
    String name;
    String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public CountBean(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
