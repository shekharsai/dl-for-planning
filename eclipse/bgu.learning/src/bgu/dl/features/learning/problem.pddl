(define (problem BLOCKS-10-0)
(:domain BLOCKS)
(:objects D A H G B J E I F C )
(:init (ontable i)(ontable f)(on g h)(on h a)(on a d)(on d i)(clear c)(ontable c)(on e f)(clear g)(clear b)(ontable b)(holding j)(clear e))
(:goal (AND (ON D C) (ON C F) (ON F J) (ON J E) (ON E H) (ON H B) (ON B A)
            (ON A G) (ON G I)))
)
