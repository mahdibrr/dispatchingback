cron-job.org — 1-minute warm-up pings

cron-job.org supports free 1-minute URL pings and is a good choice to avoid Render Free spin-down.

Steps
1. Create an account at https://cron-job.org/.
2. Add a new cron job:
   - URL: `https://your-app.onrender.com/warmup`
   - Method: `GET`
   - Interval: `1 minute`
   - Timeout: 10s
   - Save and enable the job.

Notes
- Keep `/warmup` minimal: return 204, no DB or templates.
- Monitor request logs and adjust if your app sees unexpected load from pings.
