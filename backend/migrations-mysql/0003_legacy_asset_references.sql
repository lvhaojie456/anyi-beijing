UPDATE users
SET avatar_url = REPLACE(avatar_url, 'http://101.42.1.45', 'https://api.anyibj.cn')
WHERE avatar_url LIKE '%101.42.1.45%';

UPDATE memorials
SET image_url = REPLACE(image_url, 'http://101.42.1.45', 'https://api.anyibj.cn')
WHERE image_url LIKE '%101.42.1.45%';

UPDATE assets
SET url = REPLACE(url, 'http://101.42.1.45', 'https://api.anyibj.cn')
WHERE url LIKE '%101.42.1.45%';

UPDATE community_posts
SET image_urls = REPLACE(image_urls, 'http://101.42.1.45', 'https://api.anyibj.cn')
WHERE image_urls LIKE '%101.42.1.45%';

UPDATE community_volunteer_posts
SET image_url = REPLACE(image_url, 'http://101.42.1.45', 'https://api.anyibj.cn')
WHERE image_url LIKE '%101.42.1.45%';

UPDATE ai_profiles
SET avatar_url = REPLACE(avatar_url, 'http://101.42.1.45', 'https://api.anyibj.cn'),
    smile_avatar_url = REPLACE(smile_avatar_url, 'http://101.42.1.45', 'https://api.anyibj.cn')
WHERE avatar_url LIKE '%101.42.1.45%'
   OR smile_avatar_url LIKE '%101.42.1.45%';

UPDATE ai_companions
SET avatar_url = REPLACE(avatar_url, 'http://101.42.1.45', 'https://api.anyibj.cn'),
    smile_avatar_url = REPLACE(smile_avatar_url, 'http://101.42.1.45', 'https://api.anyibj.cn')
WHERE avatar_url LIKE '%101.42.1.45%'
   OR smile_avatar_url LIKE '%101.42.1.45%';

UPDATE ritual_orders
SET acceptance_image_urls = REPLACE(acceptance_image_urls, 'http://101.42.1.45', 'https://api.anyibj.cn')
WHERE acceptance_image_urls LIKE '%101.42.1.45%';

UPDATE assets AS a
JOIN users AS u ON u.avatar_url = a.url
SET a.visibility = 'public'
WHERE EXISTS (
  SELECT 1
  FROM upload_reviews AS ur
  WHERE ur.asset_key = a.asset_key
    AND ur.status = 'approved'
    AND ur.created_at = (
      SELECT MAX(ur2.created_at)
      FROM upload_reviews AS ur2
      WHERE ur2.asset_key = a.asset_key
    )
);

UPDATE assets AS a
JOIN upload_reviews AS ur ON ur.asset_key = a.asset_key
JOIN community_posts AS p ON p.status = 'approved' AND p.image_urls LIKE CONCAT('%', a.url, '%')
SET a.visibility = 'public'
WHERE ur.status = 'approved'
  AND ur.created_at = (
    SELECT MAX(ur2.created_at)
    FROM upload_reviews AS ur2
    WHERE ur2.asset_key = a.asset_key
  );

UPDATE assets AS a
JOIN upload_reviews AS ur ON ur.asset_key = a.asset_key
JOIN community_volunteer_posts AS v ON v.image_url = a.url
SET a.visibility = 'public'
WHERE ur.status = 'approved'
  AND ur.created_at = (
    SELECT MAX(ur2.created_at)
    FROM upload_reviews AS ur2
    WHERE ur2.asset_key = a.asset_key
  );
