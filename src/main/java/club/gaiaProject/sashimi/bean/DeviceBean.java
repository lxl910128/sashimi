package club.gaiaProject.sashimi.bean;

/**
 * Created by luoxiaolong on 18-4-28.
 */
public class DeviceBean {
    private String id;//设备ID
    private String name;//设备名称
    private String ip;//设备IP
    private String typeID;//设备类型id
    private String typeName;//设备名称
    private String address;//设备地址
    private String subway;//设备站点

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getTypeID() {
        return typeID;
    }

    public void setTypeID(String typeID) {
        this.typeID = typeID;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSubway() {
        return subway;
    }

    public void setSubway(String subway) {
        this.subway = subway;
    }
}
