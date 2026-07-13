package com.classroom.repository;

import com.classroom.entity.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    /** 查询最新的一条传感器数据 */
    Optional<SensorData> findTopByOrderByCreateTimeDesc();

    /** 查询指定时间范围内的数据，按时间升序排列 */
    @Query("SELECT s FROM SensorData s WHERE s.createTime >= :startTime ORDER BY s.createTime ASC")
    List<SensorData> findDataSince(@Param("startTime") LocalDateTime startTime);

    /** 查询指定小时数内的历史数据 */
    default List<SensorData> findLastNHours(int hours) {
        return findDataSince(LocalDateTime.now().minusHours(hours));
    }

    /** 查询指定天数内的历史数据 */
    default List<SensorData> findLastNDays(int days) {
        return findDataSince(LocalDateTime.now().minusDays(days));
    }
}
