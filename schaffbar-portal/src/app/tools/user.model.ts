import { ToolCertificationStatus } from '../shared/models/tool-certification-status';

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
}

// ----------------------------------------------------------------------------
// instructors

export interface ToolInstructor extends User {
  // certifications of this tool the instructor entered
  certificationsGiven: number;
}

// a user who can become an instructor of the tool, with the own certification of the tool
export interface InstructorCandidate extends User {
  certification: { status: ToolCertificationStatus; certifiedAt: Date } | null;
}
