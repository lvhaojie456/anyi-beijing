-- Repair URLs created before the production HTTPS and asset-visibility rules.
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

-- SQLite has no UPDATE ... JOIN syntax. Use correlated EXISTS queries so this
-- migration has the same visibility rules as the MySQL deployment.
UPDATE assets
SET visibility = 'public'
WHERE EXISTS (
  SELECT 1
  FROM users AS u
  WHERE u.avatar_url = assets.url
)
AND EXISTS (
  SELECT 1
  FROM upload_reviews AS ur
  WHERE ur.asset_key = assets.asset_key
    AND ur.status = 'approved'
    AND ur.created_at = (
      SELECT MAX(ur2.created_at)
      FROM upload_reviews AS ur2
      WHERE ur2.asset_key = assets.asset_key
    )
);

UPDATE assets
SET visibility = 'public'
WHERE EXISTS (
  SELECT 1
  FROM community_posts AS p
  WHERE p.status = 'approved'
    AND instr(COALESCE(p.image_urls, ''), assets.url) > 0
)
AND EXISTS (
  SELECT 1
  FROM upload_reviews AS ur
  WHERE ur.asset_key = assets.asset_key
    AND ur.status = 'approved'
    AND ur.created_at = (
      SELECT MAX(ur2.created_at)
      FROM upload_reviews AS ur2
      WHERE ur2.asset_key = assets.asset_key
    )
);

UPDATE assets
SET visibility = 'public'
WHERE EXISTS (
  SELECT 1
  FROM community_volunteer_posts AS v
  WHERE v.image_url = assets.url
)
AND EXISTS (
  SELECT 1
  FROM upload_reviews AS ur
  WHERE ur.asset_key = assets.asset_key
    AND ur.status = 'approved'
    AND ur.created_at = (
      SELECT MAX(ur2.created_at)
      FROM upload_reviews AS ur2
      WHERE ur2.asset_key = assets.asset_key
    )
);
