USE campusfit;
SELECT
  COUNT(*) AS total,
  SUM(CASE WHEN target_grade IS NULL THEN 1 ELSE 0 END) AS null_count,
  SUM(CASE WHEN target_grade = 1 THEN 1 ELSE 0 END) AS g1,
  SUM(CASE WHEN target_grade = 2 THEN 1 ELSE 0 END) AS g2,
  SUM(CASE WHEN target_grade = 3 THEN 1 ELSE 0 END) AS g3,
  SUM(CASE WHEN target_grade = 4 THEN 1 ELSE 0 END) AS g4
FROM lectures;
SELECT c.name, l.target_grade, l.lecture_number
FROM lectures l JOIN courses c ON l.course_id = c.id
WHERE c.name IN ('자료구조(1)', '알고리즘', '컴퓨터공학캡스톤디자인(2)', '교직실무')
LIMIT 15;
