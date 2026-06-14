package com.tandian.system.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 高德地图服务类
 * 
 * @author tandian-system
 * @version 1.0.0
 */
@Slf4j
@Service
public class AmapService {

    @Value("${amap.api-key}")
    private String apiKey;

    @Value("${amap.enabled:false}")
    private Boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    /** 地理编码API */
    private static final String GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";

    /** 步行路径规划API */
    private static final String WALKING_URL = "https://restapi.amap.com/v3/direction/walking";

    /** 驾车路径规划API */
    private static final String DRIVING_URL = "https://restapi.amap.com/v3/direction/driving";

    /** 公交路径规划API */
    private static final String TRANSIT_URL = "https://restapi.amap.com/v3/direction/transit/integrated";

    /** POI搜索API */
    private static final String POI_SEARCH_URL = "https://restapi.amap.com/v3/place/text";

    @Value("${amap.security-key:}")
    private String securityKey;

    /**
     * 地理编码：地址转经纬度
     * 
     * @param address 地址文本
     * @param city 城市名称（可选）
     * @return 经纬度坐标 [经度, 纬度]
     */
    public double[] geocode(String address, String city) {
        if (!enabled) {
            log.warn("高德地图API未启用，返回默认坐标");
            return new double[]{121.4737, 31.2304}; // 默认上海坐标
        }

        try {
            log.info("【高德地图API】地理编码：{}", address);
            
            String url = String.format("%s?key=%s&address=%s&city=%s&output=json",
                    GEOCODE_URL, apiKey, address, city != null ? city : "");

            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSONObject.parseObject(response);

            if ("1".equals(json.getString("status"))) {
                JSONArray geocodes = json.getJSONArray("geocodes");
                if (geocodes != null && !geocodes.isEmpty()) {
                    JSONObject geocode = geocodes.getJSONObject(0);
                    String location = geocode.getString("location");
                    String[] parts = location.split(",");
                    
                    double lng = Double.parseDouble(parts[0]);
                    double lat = Double.parseDouble(parts[1]);
                    
                    log.info("【高德地图API】地理编码成功：{} -> [{}, {}]", address, lng, lat);
                    return new double[]{lng, lat};
                }
            }

            log.warn("【高德地图API】地理编码失败：{}", json.getString("info"));
            return null;
        } catch (Exception e) {
            log.error("【高德地图API】地理编码异常", e);
            return null;
        }
    }

    /**
     * POI关键词搜索：根据店铺名称搜索地址和坐标
     * 
     * @param keywords 关键词（店铺名称）
     * @param city 城市名称（可选）
     * @return 搜索结果 {name: 名称, address: 地址, longitude: 经度, latitude: 纬度}
     */
    public Map<String, Object> searchPOI(String keywords, String city) {
        if (!enabled) {
            log.warn("高德地图API未启用，无法搜索POI");
            return null;
        }

        try {
            log.info("【高德地图API】POI搜索：{}", keywords);
            
            String url = String.format("%s?key=%s&keywords=%s&city=%s&output=json&offset=1",
                    POI_SEARCH_URL, apiKey, keywords, city != null ? city : "四川省");

            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSONObject.parseObject(response);

            if ("1".equals(json.getString("status"))) {
                JSONArray poiList = json.getJSONArray("pois");
                if (poiList != null && !poiList.isEmpty()) {
                    JSONObject poi = poiList.getJSONObject(0);
                    Map<String, Object> result = new HashMap<>();
                    result.put("name", poi.getString("name"));
                    result.put("address", poi.getString("address"));
                    result.put("phone", poi.getString("tel"));
                    result.put("remark", poi.getString("keytag"));
                    String location = poi.getString("location");
                    if (location != null && location.contains(",")) {
                        String[] parts = location.split(",");
                        result.put("longitude", Double.parseDouble(parts[0]));
                        result.put("latitude", Double.parseDouble(parts[1]));
                    }

                    JSONObject bizExt = poi.getJSONObject("biz_ext");
                    if(!ObjectUtils.isEmpty(bizExt)){
                        result.put("business_hour", bizExt.get("open_time"));
                    }

                    log.info("【高德地图API】POI搜索成功：{} -> {}", keywords, result.get("address"));
                    return result;
                }
            }

            log.warn("【高德地图API】POI搜索失败或无结果：{}", keywords);
            return null;
        } catch (Exception e) {
            log.error("【高德地图API】POI搜索异常", e);
            return null;
        }
    }

    /**
     * 步行路径规划
     * 
     * @param originLng 起点经度
     * @param originLat 起点纬度
     * @param destLng 终点经度
     * @param destLat 终点纬度
     * @return 路径规划结果
     */
    public Map<String, Object> walkingRoute(double originLng, double originLat, 
                                            double destLng, double destLat) {
        if (!enabled) {
            log.warn("高德地图API未启用，返回模拟数据");
            return generateMockRoute(originLng, originLat, destLng, destLat, "walking");
        }

        try {
            log.info("【高德地图API】步行路径规划：({},{}) -> ({},{})", 
                    originLng, originLat, destLng, destLat);

            String url = String.format("%s?key=%s&origin=%f,%f&destination=%f,%f&output=json",
                    WALKING_URL, apiKey, originLng, originLat, destLng, destLat);

            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSONObject.parseObject(response);

            if ("1".equals(json.getString("status"))) {
                JSONObject route = json.getJSONObject("route");
                JSONObject path = route.getJSONArray("paths").getJSONObject(0);

                Map<String, Object> result = new HashMap<>();
                result.put("distance", Integer.parseInt(path.getString("distance"))); // 米
                result.put("duration", Integer.parseInt(path.getString("duration"))); // 秒
                result.put("cost", 0.0); // 步行免费
                result.put("mode", "walking");
                result.put("instruction", generateWalkingInstruction(path));
                result.put("steps", parseWalkingSteps(path.getJSONArray("steps")));

                log.info("【高德地图API】步行路径规划成功：{}米，{}秒", 
                        result.get("distance"), result.get("duration"));
                return result;
            }

            log.warn("【高德地图API】步行路径规划失败：{}", json.getString("info"));
            return null;
        } catch (Exception e) {
            log.error("【高德地图API】步行路径规划异常", e);
            return generateMockRoute(originLng, originLat, destLng, destLat, "walking");
        }
    }

    /**
     * 驾车路径规划
     * 
     * @param originLng 起点经度
     * @param originLat 起点纬度
     * @param destLng 终点经度
     * @param destLat 终点纬度
     * @return 路径规划结果（包含完整路径坐标）
     */
    public Map<String, Object> drivingRoute(double originLng, double originLat,
                                             double destLng, double destLat) {
        if (!enabled) {
            log.warn("高德地图API未启用，返回模拟数据");
            return generateMockRoute(originLng, originLat, destLng, destLat, "driving");
        }

        try {
            log.info("【高德地图API】驾车路径规划：({},{}) -> ({},{})",
                    originLng, originLat, destLng, destLat);

            String url = String.format("%s?key=%s&origin=%f,%f&destination=%f,%f&output=json&extensions=all",
                    DRIVING_URL, apiKey, originLng, originLat, destLng, destLat);

            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSONObject.parseObject(response);

            if ("1".equals(json.getString("status"))) {
                JSONObject route = json.getJSONObject("route");
                JSONObject path = route.getJSONArray("paths").getJSONObject(0);

                Map<String, Object> result = new HashMap<>();
                result.put("distance", Integer.parseInt(path.getString("distance"))); // 米
                result.put("duration", Integer.parseInt(path.getString("duration"))); // 秒
                result.put("cost", 0.0); // 驾车暂不计费
                result.put("mode", "driving");
                result.put("instruction", generateDrivingInstruction(path));
                result.put("steps", parseDrivingSteps(path.getJSONArray("steps")));
                result.put("polyline", extractPolyline(path.getJSONArray("steps")));

                log.info("【高德地图API】驾车路径规划成功：{}米，{}秒",
                        result.get("distance"), result.get("duration"));
                return result;
            }

            log.warn("【高德地图API】驾车路径规划失败：{}", json.getString("info"));
            return null;
        } catch (Exception e) {
            log.error("【高德地图API】驾车路径规划异常", e);
            return generateMockRoute(originLng, originLat, destLng, destLat, "driving");
        }
    }

    /**
     * 公交路径规划（优先地铁）
     * 
     * @param originLng 起点经度
     * @param originLat 起点纬度
     * @param destLng 终点经度
     * @param destLat 终点纬度
     * @param city 城市名称
     * @param strategy 策略：0-最快捷，1-最经济，2-最少换乘，3-最少步行，4-不乘地铁，5-地铁优先
     * @return 路径规划结果（包含完整路径坐标和换乘信息）
     */
    public Map<String, Object> transitRoute(double originLng, double originLat, 
                                            double destLng, double destLat, 
                                            String city, Integer strategy) {
        if (!enabled) {
            log.warn("高德地图API未启用，返回模拟数据");
            return generateMockRoute(originLng, originLat, destLng, destLat, "transit");
        }

        try {
            log.info("【高德地图API】公交路径规划：({},{}) -> ({},{})，策略：{}", 
                    originLng, originLat, destLng, destLat, strategy);

            // 默认使用地铁优先策略（strategy=5）
            int actualStrategy = (strategy != null) ? strategy : 5;
            
            String url = String.format("%s?key=%s&origin=%f,%f&destination=%f,%f&city=%s&strategy=%d&output=json&extensions=all",
                    TRANSIT_URL, apiKey, originLng, originLat, destLng, destLat, city, actualStrategy);

            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSONObject.parseObject(response);

            if ("1".equals(json.getString("status"))) {
                JSONObject route = json.getJSONObject("route");
                JSONArray transits = route.getJSONArray("transits");
                
                if (transits == null || transits.isEmpty()) {
                    log.warn("【高德地图API】公交路径规划无结果");
                    return null;
                }
                
                // 选择第一个方案（最优）
                JSONObject transit = transits.getJSONObject(0);

                Map<String, Object> result = new HashMap<>();
                result.put("distance", Integer.parseInt(route.getString("distance"))); // 米
                result.put("duration", Integer.parseInt(transit.getString("duration"))); // 秒
                result.put("cost", parseTransitCost(transit)); // 元
                result.put("mode", "transit");
                result.put("instruction", generateTransitInstruction(transit));
                result.put("steps", parseTransitStepsV2(transit.getJSONArray("segments")));
                result.put("polyline", extractTransitPolyline(transit.getJSONArray("segments")));
                result.put("transitDetails", parseTransitDetails(transit.getJSONArray("segments")));

                log.info("【高德地图API】公交路径规划成功：{}米，{}秒，{}元", 
                        result.get("distance"), result.get("duration"), result.get("cost"));
                return result;
            }

            log.warn("【高德地图API】公交路径规划失败：{}", json.getString("info"));
            return null;
        } catch (Exception e) {
            log.error("【高德地图API】公交路径规划异常", e);
            return generateMockRoute(originLng, originLat, destLng, destLat, "transit");
        }
    }

    /**
     * 生成步行指引文本
     */
    private String generateWalkingInstruction(JSONObject path) {
        int distance = Integer.parseInt(path.getString("distance"));
        int duration = Integer.parseInt(path.getString("duration"));
        int minutes = duration / 60;

        return String.format("步行约%.1f公里，预计%d分钟", distance / 1000.0, minutes);
    }

    /**
     * 解析步行路段
     */
    private JSONArray parseWalkingSteps(JSONArray steps) {
        JSONArray result = new JSONArray();
        if (steps == null) return result;

        for (int i = 0; i < steps.size(); i++) {
            JSONObject step = steps.getJSONObject(i);
            JSONObject stepInfo = new JSONObject();
            stepInfo.put("instruction", step.getString("instruction"));
            stepInfo.put("road", step.getString("road"));
            stepInfo.put("distance", Integer.parseInt(step.getString("distance")));
            stepInfo.put("duration", Integer.parseInt(step.getString("duration")));
            result.add(stepInfo);
        }

        return result;
    }

    /**
     * 生成公交指引文本
     */
    private String generateTransitInstruction(JSONObject transit) {
        int duration = Integer.parseInt(transit.getString("duration"));
        int minutes = duration / 60;
        int walkingDistance = Integer.parseInt(transit.getString("walking_distance"));

        return String.format("预计%d分钟，步行约%d米", minutes, walkingDistance);
    }

    /**
     * 解析公交花费
     */
    private double parseTransitCost(JSONObject transit) {
        try {
            String cost = transit.getString("cost");
            return cost != null ? Double.parseDouble(cost) : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * 解析公交路段
     */
    private JSONArray parseTransitSteps(JSONArray segments) {
        JSONArray result = new JSONArray();
        if (segments == null) return result;

        for (int i = 0; i < segments.size(); i++) {
            JSONObject segment = segments.getJSONObject(i);
            JSONObject stepInfo = new JSONObject();

            // 步行部分
            if (segment.containsKey("walking")) {
                JSONObject walking = segment.getJSONObject("walking");
                stepInfo.put("type", "walking");
                stepInfo.put("distance", Integer.parseInt(walking.getString("distance")));
                stepInfo.put("duration", Integer.parseInt(walking.getString("duration")));
                stepInfo.put("instruction", "步行" + walking.getString("distance") + "米");
            }

            // 公交部分
            if (segment.containsKey("bus")) {
                JSONObject bus = segment.getJSONObject("bus");
                JSONObject busLine = bus.getJSONArray("buslines").getJSONObject(0);
                stepInfo.put("type", "bus");
                stepInfo.put("line", busLine.getString("name"));
                stepInfo.put("departure_stop", busLine.getString("departure_stop"));
                stepInfo.put("arrival_stop", busLine.getString("arrival_stop"));
                stepInfo.put("via_num", busLine.getString("via_num"));
                stepInfo.put("instruction", "乘坐" + busLine.getString("name"));
            }

            result.add(stepInfo);
        }

        return result;
    }

    /**
     * 生成模拟路径数据（当API不可用时）
     */
    private Map<String, Object> generateMockRoute(double originLng, double originLat,
                                                   double destLng, double destLat, String mode) {
        // 计算直线距离
        double dx = (destLng - originLng) * 111000; // 大约每度111公里
        double dy = (destLat - originLat) * 111000;
        double distance = Math.sqrt(dx * dx + dy * dy);

        Map<String, Object> result = new HashMap<>();
        result.put("distance", (int) distance);
        result.put("duration", (int) (distance / 80 * 60)); // 步行速度约80米/分钟
        result.put("cost", 0.0);
        result.put("mode", mode);
        result.put("instruction", String.format("%s约%.1f公里", 
                "walking".equals(mode) ? "步行" : "公交", distance / 1000));
        result.put("steps", new JSONArray());
        
        // 生成简单的直线路径
        JSONArray polyline = new JSONArray();
        JSONObject start = new JSONObject();
        start.put("lng", originLng);
        start.put("lat", originLat);
        polyline.add(start);
        JSONObject end = new JSONObject();
        end.put("lng", destLng);
        end.put("lat", destLat);
        polyline.add(end);
        result.put("polyline", polyline);

        return result;
    }

    /**
     * 生成驾车指引文本
     */
    private String generateDrivingInstruction(JSONObject path) {
        int distance = Integer.parseInt(path.getString("distance"));
        int duration = Integer.parseInt(path.getString("duration"));
        int minutes = duration / 60;

        return String.format("驾车约%.1f公里，预计%d分钟", distance / 1000.0, minutes);
    }

    /**
     * 解析驾车路段
     */
    private JSONArray parseDrivingSteps(JSONArray steps) {
        JSONArray result = new JSONArray();
        if (steps == null) return result;

        for (int i = 0; i < steps.size(); i++) {
            JSONObject step = steps.getJSONObject(i);
            JSONObject stepInfo = new JSONObject();
            stepInfo.put("instruction", step.getString("instruction"));
            stepInfo.put("road", step.getString("road"));
            stepInfo.put("distance", Integer.parseInt(step.getString("distance")));
            stepInfo.put("duration", Integer.parseInt(step.getString("duration")));
            result.add(stepInfo);
        }

        return result;
    }

    /**
     * 从驾车路段中提取路径坐标
     */
    private JSONArray extractPolyline(JSONArray steps) {
        JSONArray polyline = new JSONArray();
        if (steps == null) return polyline;

        for (int i = 0; i < steps.size(); i++) {
            JSONObject step = steps.getJSONObject(i);
            String polylineStr = step.getString("polyline");
            if (polylineStr != null && !polylineStr.isEmpty()) {
                // 解析坐标字符串："lng,lat;lng,lat;..."
                String[] points = polylineStr.split(";");
                for (String point : points) {
                    String[] coords = point.split(",");
                    if (coords.length >= 2) {
                        JSONObject coord = new JSONObject();
                        coord.put("lng", Double.parseDouble(coords[0]));
                        coord.put("lat", Double.parseDouble(coords[1]));
                        polyline.add(coord);
                    }
                }
            }
        }

        return polyline;
    }

    /**
     * 解析公交路段（新版，包含更多详情）
     */
    private JSONArray parseTransitStepsV2(JSONArray segments) {
        JSONArray result = new JSONArray();
        if (segments == null) return result;

        for (int i = 0; i < segments.size(); i++) {
            JSONObject segment = segments.getJSONObject(i);
            JSONObject stepInfo = new JSONObject();

            // 步行部分
            if (segment.containsKey("walking")) {
                JSONObject walking = segment.getJSONObject("walking");
                stepInfo.put("type", "walking");
                stepInfo.put("distance", Integer.parseInt(walking.getString("distance")));
                stepInfo.put("duration", Integer.parseInt(walking.getString("duration")));
                stepInfo.put("instruction", "步行" + walking.getString("distance") + "米");
                
                // 提取步行路径
                if (walking.containsKey("steps")) {
                    stepInfo.put("polyline", extractWalkingPolyline(walking.getJSONArray("steps")));
                }
            }

            // 公交/地铁部分
            if (segment.containsKey("bus")) {
                JSONObject bus = segment.getJSONObject("bus");
                if(!ObjectUtils.isEmpty(bus) && !ObjectUtils.isEmpty(bus.getJSONArray("buslines"))){
                    JSONObject busLine = bus.getJSONArray("buslines").getJSONObject(0);

                    // 打印完整数据用于调试
                    log.info("【公交/地铁数据】{}", busLine.toJSONString());

                    String busType = busLine.getString("type");
                    boolean isSubway = "地铁线路".equals(busType) || "1".equals(busLine.getString("type"));

                    stepInfo.put("type", isSubway ? "subway" : "bus");
                    stepInfo.put("line", busLine.getString("name"));
                    stepInfo.put("departure_stop", busLine.getString("departure_stop"));
                    stepInfo.put("arrival_stop", busLine.getString("arrival_stop"));
                    stepInfo.put("via_num", busLine.getString("via_num"));

                    // 提取方向信息（可能在不同字段中）
                    String direction = busLine.getString("direction");
                    if (direction == null || direction.isEmpty()) {
                        direction = busLine.getString("destination");
                    }
                    if (direction == null || direction.isEmpty()) {
                        // 从name中提取方向，如"地铁1号线(莘庄方向)"
                        String name = busLine.getString("name");
                        if (name != null && name.contains("(")) {
                            int start = name.indexOf("(");
                            int end = name.indexOf(")");
                            if (start > 0 && end > start) {
                                direction = name.substring(start + 1, end);
                            }
                        }
                    }
                    stepInfo.put("direction", direction != null ? direction : "");

                    // 构建更详细的指引
                    String instruction = "乘坐" + busLine.getString("name");
                    if (direction != null && !direction.isEmpty()) {
                        instruction += "（" + direction + "）";
                    }
                    instruction += "，" + busLine.getString("departure_stop") + " → " + busLine.getString("arrival_stop");
                    stepInfo.put("instruction", instruction);

                    stepInfo.put("distance", Integer.parseInt(busLine.getString("distance")));
                    stepInfo.put("duration", Integer.parseInt(busLine.getString("duration")));
                }
            }

            result.add(stepInfo);
        }

        return result;
    }

    /**
     * 从公交路段中提取完整路径坐标
     */
    private JSONArray extractTransitPolyline(JSONArray segments) {
        JSONArray polyline = new JSONArray();
        if (segments == null) return polyline;

        for (int i = 0; i < segments.size(); i++) {
            JSONObject segment = segments.getJSONObject(i);

            // 步行路径
            if (segment.containsKey("walking")) {
                JSONObject walking = segment.getJSONObject("walking");
                if (walking.containsKey("steps")) {
                    JSONArray walkSteps = walking.getJSONArray("steps");
                    for (int j = 0; j < walkSteps.size(); j++) {
                        JSONObject step = walkSteps.getJSONObject(j);
                        String polylineStr = step.getString("polyline");
                        if (polylineStr != null && !polylineStr.isEmpty()) {
                            addPolylineCoords(polyline, polylineStr);
                        }
                    }
                }
            }

            // 公交/地铁路径
            if (segment.containsKey("bus")) {
                JSONObject bus = segment.getJSONObject("bus");
                if(!ObjectUtils.isEmpty(bus) && !ObjectUtils.isEmpty(bus.getJSONArray("buslines"))) {
                    JSONObject busLine = bus.getJSONArray("buslines").getJSONObject(0);
                    String polylineStr = busLine.getString("polyline");
                    if (polylineStr != null && !polylineStr.isEmpty()) {
                        addPolylineCoords(polyline, polylineStr);
                    }
                }
            }
        }

        return polyline;
    }

    /**
     * 解析公交换乘详情
     */
    private JSONArray parseTransitDetails(JSONArray segments) {
        JSONArray details = new JSONArray();
        if (segments == null) return details;

        for (int i = 0; i < segments.size(); i++) {
            JSONObject segment = segments.getJSONObject(i);
            JSONObject detail = new JSONObject();

            // 步行部分
            if (segment.containsKey("walking")) {
                JSONObject walking = segment.getJSONObject("walking");
                detail.put("type", "walking");
                detail.put("distance", walking.getString("distance") + "米");
                detail.put("duration", walking.getString("duration") + "秒");
                detail.put("desc", "步行" + walking.getString("distance") + "米");
            }

            // 公交/地铁部分
            if (segment.containsKey("bus")) {
                JSONObject bus = segment.getJSONObject("bus");
                if (!ObjectUtils.isEmpty(bus) && !ObjectUtils.isEmpty(bus.getJSONArray("buslines"))) {
                    JSONObject busLine = bus.getJSONArray("buslines").getJSONObject(0);

                    String busType = busLine.getString("type");
                    boolean isSubway = "地铁线路".equals(busType) || "1".equals(busLine.getString("type"));

                    detail.put("type", isSubway ? "subway" : "bus");
                    detail.put("lineName", busLine.getString("name"));
                    detail.put("departureStop", busLine.getString("departure_stop"));
                    detail.put("arrivalStop", busLine.getString("arrival_stop"));
                    detail.put("viaStops", busLine.getString("via_num") + "站");
                    detail.put("price", busLine.containsKey("cost") ? "¥" + busLine.getString("cost") : "未知");

                    // 提取方向信息（可能在不同字段中）
                    String direction = busLine.getString("direction");
                    if (direction == null || direction.isEmpty()) {
                        direction = busLine.getString("destination");
                    }
                    if (direction == null || direction.isEmpty()) {
                        // 从name中提取方向，如"地铁1号线(莘庄方向)"
                        String name = busLine.getString("name");
                        if (name != null && name.contains("(")) {
                            int start = name.indexOf("(");
                            int end = name.indexOf(")");
                            if (start > 0 && end > start) {
                                direction = name.substring(start + 1, end);
                            }
                        }
                    }
                    if (direction != null && !direction.isEmpty()) {
                        detail.put("direction", direction);
                    }

                    // 构建详细描述
                    String desc = busLine.getString("name");
                    if (direction != null && !direction.isEmpty()) {
                        desc += "（" + direction + "）";
                    }
                    desc += ": " + busLine.getString("departure_stop") + " → " + busLine.getString("arrival_stop");
                    detail.put("desc", desc);
                }
            }

            details.add(detail);
        }

        return details;
    }

    /**
     * 从步行路段中提取路径坐标
     */
    private JSONArray extractWalkingPolyline(JSONArray steps) {
        JSONArray polyline = new JSONArray();
        if (steps == null) return polyline;

        for (int i = 0; i < steps.size(); i++) {
            JSONObject step = steps.getJSONObject(i);
            String polylineStr = step.getString("polyline");
            if (polylineStr != null && !polylineStr.isEmpty()) {
                addPolylineCoords(polyline, polylineStr);
            }
        }

        return polyline;
    }

    /**
     * 将坐标字符串添加到路径数组
     */
    private void addPolylineCoords(JSONArray polyline, String polylineStr) {
        String[] points = polylineStr.split(";");
        for (String point : points) {
            String[] coords = point.split(",");
            if (coords.length >= 2) {
                JSONObject coord = new JSONObject();
                coord.put("lng", Double.parseDouble(coords[0]));
                coord.put("lat", Double.parseDouble(coords[1]));
                polyline.add(coord);
            }
        }
    }
}
