-- ============================================
-- 学生表结构优化：添加班级和年级外键关联
-- 执行顺序：1. 结构修改 2. 数据迁移 3. 添加约束 4. 创建触发器
-- ============================================

USE camp;

-- ============================================
-- 步骤1：添加外键字段（允许为空，便于数据迁移）
-- ============================================
ALTER TABLE students 
ADD COLUMN class_id BIGINT COMMENT '班级ID（外键）' AFTER class_name,
ADD COLUMN grade_id BIGINT COMMENT '年级ID（外键，冗余但方便查询）' AFTER grade;

-- ============================================
-- 步骤2：添加索引
-- ============================================
ALTER TABLE students 
ADD INDEX idx_class_id (class_id),
ADD INDEX idx_grade_id (grade_id);

-- ============================================
-- 步骤3：数据迁移：根据现有 grade 和 class_name 匹配并填充外键
-- ============================================

-- 3.1 先更新 grade_id（通过年级名称或年级代码匹配）
UPDATE students s
LEFT JOIN grades g ON (
    s.grade = g.grade_name 
    OR s.grade = g.grade_code
    OR s.grade LIKE CONCAT('%', g.grade_name, '%')
    OR s.grade LIKE CONCAT('%', g.grade_code, '%')
)
SET s.grade_id = g.id
WHERE g.id IS NOT NULL;

-- 3.2 再更新 class_id（通过班级名称和年级匹配）
UPDATE students s
LEFT JOIN grades g ON s.grade_id = g.id
LEFT JOIN classes c ON (
    s.class_name = c.class_name 
    AND c.grade_id = g.id
)
SET s.class_id = c.id
WHERE c.id IS NOT NULL;

-- ============================================
-- 步骤4：查看迁移结果统计
-- ============================================
SELECT 
    COUNT(*) as total_students,
    COUNT(grade_id) as students_with_grade_id,
    COUNT(class_id) as students_with_class_id,
    COUNT(*) - COUNT(grade_id) as missing_grade_id,
    COUNT(*) - COUNT(class_id) as missing_class_id
FROM students;

-- ============================================
-- 步骤5：添加外键约束（在数据迁移完成后执行）
-- 注意：如果还有数据未匹配成功，外键约束会失败
-- 建议先处理未匹配的数据，再执行此步骤
-- ============================================
-- ALTER TABLE students 
-- ADD CONSTRAINT fk_student_class 
--     FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE SET NULL,
-- ADD CONSTRAINT fk_student_grade 
--     FOREIGN KEY (grade_id) REFERENCES grades(id) ON DELETE SET NULL;

-- ============================================
-- 步骤6：创建触发器自动同步冗余字段
-- 当 class_id 或 grade_id 更新时，自动更新 grade 和 class_name
-- ============================================
DELIMITER $$

DROP TRIGGER IF EXISTS trg_student_sync_grade_class_on_update$$
CREATE TRIGGER trg_student_sync_grade_class_on_update
BEFORE UPDATE ON students
FOR EACH ROW
BEGIN
    -- 如果 class_id 被更新，同步 class_name、grade_id 和 grade
    IF NEW.class_id IS NOT NULL AND (NEW.class_id != OLD.class_id OR OLD.class_id IS NULL) THEN
        SELECT c.class_name, c.grade_id, g.grade_name 
        INTO NEW.class_name, NEW.grade_id, NEW.grade
        FROM classes c
        JOIN grades g ON c.grade_id = g.id
        WHERE c.id = NEW.class_id
        LIMIT 1;
    END IF;
    
    -- 如果 grade_id 被更新，同步 grade
    IF NEW.grade_id IS NOT NULL AND (NEW.grade_id != OLD.grade_id OR OLD.grade_id IS NULL) AND NEW.grade IS NULL THEN
        SELECT grade_name INTO NEW.grade 
        FROM grades 
        WHERE id = NEW.grade_id
        LIMIT 1;
    END IF;
END$$

DROP TRIGGER IF EXISTS trg_student_sync_grade_class_on_insert$$
CREATE TRIGGER trg_student_sync_grade_class_on_insert
BEFORE INSERT ON students
FOR EACH ROW
BEGIN
    -- 如果 class_id 被设置，同步 class_name、grade_id 和 grade
    IF NEW.class_id IS NOT NULL THEN
        SELECT c.class_name, c.grade_id, g.grade_name 
        INTO NEW.class_name, NEW.grade_id, NEW.grade
        FROM classes c
        JOIN grades g ON c.grade_id = g.id
        WHERE c.id = NEW.class_id
        LIMIT 1;
    END IF;
    
    -- 如果 grade_id 被设置，同步 grade
    IF NEW.grade_id IS NOT NULL AND NEW.grade IS NULL THEN
        SELECT grade_name INTO NEW.grade 
        FROM grades 
        WHERE id = NEW.grade_id
        LIMIT 1;
    END IF;
END$$

DELIMITER ;

