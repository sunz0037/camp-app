-- ============================================
-- 数据清理：查找并处理无法匹配的数据
-- 在执行主迁移脚本后，使用此脚本查找问题数据
-- ============================================

USE camp;

-- ============================================
-- 1. 查找无法匹配年级的学生
-- ============================================
SELECT 
    s.id,
    s.student_no,
    s.grade,
    s.class_name,
    '无法匹配年级' as issue
FROM students s
WHERE s.grade_id IS NULL 
  AND s.grade IS NOT NULL 
  AND s.grade != ''
ORDER BY s.grade, s.class_name;

-- ============================================
-- 2. 查找无法匹配班级的学生
-- ============================================
SELECT 
    s.id,
    s.student_no,
    s.grade,
    s.class_name,
    s.grade_id,
    g.grade_name as matched_grade,
    '无法匹配班级' as issue
FROM students s
LEFT JOIN grades g ON s.grade_id = g.id
WHERE s.class_id IS NULL 
  AND s.class_name IS NOT NULL 
  AND s.class_name != ''
  AND s.grade_id IS NOT NULL
ORDER BY s.grade_id, s.class_name;

-- ============================================
-- 3. 查找所有未匹配的数据（汇总）
-- ============================================
SELECT 
    'grade_id缺失' as issue_type,
    COUNT(*) as count
FROM students
WHERE grade_id IS NULL AND grade IS NOT NULL AND grade != ''

UNION ALL

SELECT 
    'class_id缺失' as issue_type,
    COUNT(*) as count
FROM students
WHERE class_id IS NULL AND class_name IS NOT NULL AND class_name != '' AND grade_id IS NOT NULL;

-- ============================================
-- 4. 手动修复示例（需要根据实际情况调整）
-- ============================================
-- 示例1：如果年级名称是"小学一年级"，但数据库中年级名称是"小学一年级"
-- UPDATE students SET grade_id = (SELECT id FROM grades WHERE grade_name = '小学一年级' LIMIT 1) 
-- WHERE grade = '小学一年级' AND grade_id IS NULL;

-- 示例2：如果班级名称是"一年级1班"，年级ID是1
-- UPDATE students SET class_id = (
--     SELECT id FROM classes 
--     WHERE class_name = '一年级1班' AND grade_id = 1 
--     LIMIT 1
-- ) 
-- WHERE class_name = '一年级1班' AND grade_id = 1 AND class_id IS NULL;

-- ============================================
-- 5. 验证修复后的数据完整性
-- ============================================
SELECT 
    COUNT(*) as total_students,
    COUNT(grade_id) as students_with_grade_id,
    COUNT(class_id) as students_with_class_id,
    ROUND(COUNT(grade_id) * 100.0 / COUNT(*), 2) as grade_id_coverage_percent,
    ROUND(COUNT(class_id) * 100.0 / COUNT(*), 2) as class_id_coverage_percent
FROM students
WHERE status = 1; -- 仅统计在读学生

