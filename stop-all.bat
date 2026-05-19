@echo off
echo ============================================
echo  perf-demo: Stopping all instances...
echo ============================================

for /f "tokens=1" %%p in ('wmic process where "commandline like '%%perf-demo-1.0.0.jar%%'" get processid 2^>nul ^| findstr /r "[0-9]"') do (
    echo Killing PID %%p
    taskkill /PID %%p /F > nul 2>&1
)

echo All perf-demo instances stopped.
echo ============================================
