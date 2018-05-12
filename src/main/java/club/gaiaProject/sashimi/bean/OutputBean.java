package club.gaiaProject.sashimi.bean;

import java.util.List;

public class OutputBean {
   String name;
   List<EventVO> eventList;
   String count;

   String subway;
   String startTime;
   String endTime;

    public String getSubway() {
        return subway;
    }

    public void setSubway(String subway) {
        this.subway = subway;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public List<EventVO> getEventList() {
        return eventList;
    }

    public void setEventList(List<EventVO> eventList) {
        this.eventList = eventList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
