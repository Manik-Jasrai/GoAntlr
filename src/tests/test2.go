package main

import (
	"fmt"
	"time"
)

// VestingSchedule represents a vesting schedule for tokens
type VestingSchedule struct {
	TotalAmount    uint64
	ReleasedAmount uint64
	StartTime      time.Time
	Duration       time.Duration
	CliffDuration  time.Duration
	IsRevocable    bool
	IsRevoked      bool
}

// TokenVesting manages token vesting schedules
type TokenVesting struct {
	owner             string
	vestingSchedules  map[string]*VestingSchedule
	beneficiaryTokens map[string]uint64
}

// NewTokenVesting creates a new token vesting contract
func NewTokenVesting(owner string) *TokenVesting {
	return &TokenVesting{
		owner:             owner,
		vestingSchedules:  make(map[string]*VestingSchedule),
		beneficiaryTokens: make(map[string]uint64),
	}
}

// CreateVestingSchedule creates a new vesting schedule for a beneficiary
func (tv *TokenVesting) CreateVestingSchedule(
	beneficiary string,
	amount uint64,
	startTime time.Time,
	duration time.Duration,
	cliffDuration time.Duration,
	isRevocable bool,
) error {
	// Input validation
	if len(beneficiary) == 0 {
		return fmt.Errorf("invalid beneficiary address")
	}

	if amount == 0 {
		return fmt.Errorf("amount must be greater than 0")
	}

	if duration == 0 {
		return fmt.Errorf("duration must be greater than 0")
	}

	if cliffDuration > duration {
		return fmt.Errorf("cliff duration must be <= duration")
	}

	// Check if vesting schedule already exists
	if _, exists := tv.vestingSchedules[beneficiary]; exists {
		return fmt.Errorf("vesting schedule already exists")
	}

	// Create new vesting schedule
	schedule := &VestingSchedule{
		TotalAmount:    amount,
		ReleasedAmount: 0,
		StartTime:      startTime,
		Duration:       duration,
		CliffDuration:  cliffDuration,
		IsRevocable:    isRevocable,
		IsRevoked:      false,
	}

	tv.vestingSchedules[beneficiary] = schedule
	return nil
}

// CalculateVestedAmount calculates the vested amount for a beneficiary
func (tv *TokenVesting) CalculateVestedAmount(beneficiary string) (uint64, error) {
	schedule, exists := tv.vestingSchedules[beneficiary]
	if !exists {
		return 0, fmt.Errorf("no vesting schedule found")
	}

	if schedule.IsRevoked {
		return 0, fmt.Errorf("vesting schedule has been revoked")
	}

	currentTime := time.Now()
	
	// Check if vesting hasn't started or is in cliff period
	if currentTime.Before(schedule.StartTime.Add(schedule.CliffDuration)) {
		return 0, nil
	}

	// Calculate time elapsed since start
	elapsed := currentTime.Sub(schedule.StartTime)
	if elapsed >= schedule.Duration {
		return schedule.TotalAmount, nil
	}

	// Calculate linear vesting amount
	vestedAmount := uint64(float64(schedule.TotalAmount) * float64(elapsed) / float64(schedule.Duration))
	return vestedAmount, nil
}

// Release releases vested tokens to the beneficiary
func (tv *TokenVesting) Release(beneficiary string) (uint64, error) {
	schedule, exists := tv.vestingSchedules[beneficiary]
	if !exists {
		return 0, fmt.Errorf("no vesting schedule found")
	}

	vestedAmount, err := tv.CalculateVestedAmount(beneficiary)
	if err != nil {
		return 0, err
	}

	// Calculate releasable amount
	releasableAmount := vestedAmount - schedule.ReleasedAmount
	if releasableAmount == 0 {
		return 0, fmt.Errorf("no tokens available for release")
	}

	// Update released amount
	schedule.ReleasedAmount += releasableAmount
	tv.beneficiaryTokens[beneficiary] += releasableAmount

	return releasableAmount, nil
}

// Revoke revokes the vesting schedule if it's revocable
func (tv *TokenVesting) Revoke(beneficiary string) error {
	schedule, exists := tv.vestingSchedules[beneficiary]
	if !exists {
		return fmt.Errorf("no vesting schedule found")
	}

	if !schedule.IsRevocable {
		return fmt.Errorf("vesting schedule is not revocable")
	}

	if schedule.IsRevoked {
		return fmt.Errorf("vesting schedule already revoked")
	}

	// Calculate vested amount before revoking
	vestedAmount, err := tv.CalculateVestedAmount(beneficiary)
	if err != nil {
		return err
	}

	// Update schedule
	schedule.IsRevoked = true
	schedule.ReleasedAmount = vestedAmount

	return nil
}

func main() {
	// Example usage
	vesting := NewTokenVesting("owner123")
	
	// Create a vesting schedule
	err := vesting.CreateVestingSchedule(
		"beneficiary1",
		1000000,
		time.Now(),
		365*24*time.Hour,  // 1 year duration
		30*24*time.Hour,   // 1 month cliff
		true,
	)
	
	if err != nil {
		fmt.Printf("Error creating schedule: %v\n", err)
		return
	}

	// Calculate vested amount
	vestedAmount, err := vesting.CalculateVestedAmount("beneficiary1")
	if err != nil {
		fmt.Printf("Error calculating vested amount: %v\n", err)
		return
	}

	fmt.Printf("Vested amount: %d\n", vestedAmount)

	// Release tokens
	releasedAmount, err := vesting.Release("beneficiary1")
	if err != nil {
		fmt.Printf("Error releasing tokens: %v\n", err)
		return
	}

	fmt.Printf("Released amount: %d\n", releasedAmount)
}